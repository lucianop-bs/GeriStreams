package com.projeto.service;

import com.projeto.dto.admin.RankingAssinaturaDTO;
import com.projeto.dto.assinatura.AssinaturaRequestDTO;
import com.projeto.dto.assinatura.AssinaturaResponseDTO;
import com.projeto.dto.financeiro.ResumoFinanceiroDTO;
import com.projeto.model.Assinatura;
import com.projeto.model.CategoriaAssinatura;
import com.projeto.model.Usuario;
import com.projeto.repository.AssinaturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final UsuarioService usuarioService;

    public AssinaturaService(AssinaturaRepository assinaturaRepository, UsuarioService usuarioService) {
        this.assinaturaRepository = assinaturaRepository;
        this.usuarioService = usuarioService;
    }

    public List<AssinaturaResponseDTO> listar() {
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        return assinaturaRepository.findByUsuarioId(usuario.getId()).stream()
                .map(AssinaturaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public AssinaturaResponseDTO criar(AssinaturaRequestDTO dto) {
        Usuario usuario = usuarioService.getUsuarioAutenticado();

        Assinatura assinatura = new Assinatura();
        assinatura.setNome(dto.nome());
        assinatura.setValor(dto.valor());
        assinatura.setCategoria(dto.categoria());
        assinatura.setUsuario(usuario);

        return AssinaturaResponseDTO.fromEntity(assinaturaRepository.save(assinatura));
    }

    @Transactional
    public AssinaturaResponseDTO atualizar(Long id, AssinaturaRequestDTO dto) {
        Assinatura assinatura = buscarPorIdDoUsuario(id);
        assinatura.setNome(dto.nome());
        assinatura.setValor(dto.valor());
        assinatura.setCategoria(dto.categoria());
        return AssinaturaResponseDTO.fromEntity(assinaturaRepository.save(assinatura));
    }

    @Transactional
    public void remover(Long id) {
        Assinatura assinatura = buscarPorIdDoUsuario(id);
        assinaturaRepository.delete(assinatura);
    }

    @Transactional
    public AssinaturaResponseDTO alternarAtivo(Long id) {
        Assinatura assinatura = buscarPorIdDoUsuario(id);
        assinatura.setAtivo(!assinatura.getAtivo());
        return AssinaturaResponseDTO.fromEntity(assinaturaRepository.save(assinatura));
    }

    public ResumoFinanceiroDTO calcularResumoFinanceiro() {
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        Long usuarioId = usuario.getId();

        List<AssinaturaResponseDTO> assinaturas = assinaturaRepository
                .findByUsuarioIdAndAtivoTrue(usuarioId).stream()
                .map(AssinaturaResponseDTO::fromEntity)
                .toList();

        BigDecimal total = assinaturaRepository.sumValorAtivoByUsuarioId(usuarioId);
        if (total == null) total = BigDecimal.ZERO;

        BigDecimal salario = usuario.getSalario();
        BigDecimal percentual = salario.compareTo(BigDecimal.ZERO) > 0
                ? total.divide(salario, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        Map<String, BigDecimal> gastosPorCategoria = new LinkedHashMap<>();
        assinaturaRepository.sumValorGroupedByCategoriaAndUsuarioId(usuarioId)
                .forEach(row -> gastosPorCategoria.put(
                        ((CategoriaAssinatura) row[0]).name(),
                        (BigDecimal) row[1]
                ));

        return new ResumoFinanceiroDTO(salario, total, percentual, assinaturas, gastosPorCategoria);
    }

    public List<RankingAssinaturaDTO> rankingServicos() {
        return assinaturaRepository.rankingServicos().stream()
                .map(row -> new RankingAssinaturaDTO(
                        (String) row[0],
                        (Long) row[1],
                        (BigDecimal) row[2],
                        (BigDecimal) row[3]
                ))
                .toList();
    }

    public List<AssinaturaResponseDTO> listarPorUsuario(Long usuarioId) {
        return assinaturaRepository.findByUsuarioId(usuarioId).stream()
                .map(AssinaturaResponseDTO::fromEntity)
                .toList();
    }

    private Assinatura buscarPorIdDoUsuario(Long id) {
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        return assinaturaRepository.findById(id)
                .filter(a -> a.getUsuario().getId().equals(usuario.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada."));
    }
}
