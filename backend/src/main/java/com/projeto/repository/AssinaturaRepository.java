package com.projeto.repository;

import com.projeto.model.Assinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    List<Assinatura> findByUsuarioId(Long usuarioId);

    List<Assinatura> findByUsuarioIdAndAtivoTrue(Long usuarioId);

    @Query("SELECT SUM(a.valor) FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.ativo = true")
    BigDecimal sumValorAtivoByUsuarioId(Long usuarioId);

    @Query("SELECT a.categoria, SUM(a.valor) FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.ativo = true GROUP BY a.categoria")
    List<Object[]> sumValorGroupedByCategoriaAndUsuarioId(Long usuarioId);

    @Query("SELECT a.usuario.id, SUM(a.valor) FROM Assinatura a WHERE a.ativo = true GROUP BY a.usuario.id ORDER BY SUM(a.valor) DESC")
    List<Object[]> rankingGastosPorUsuario();

    @Query("""
            SELECT a.nome, COUNT(a), SUM(a.valor), AVG(a.valor)
            FROM Assinatura a
            WHERE a.ativo = true
            GROUP BY a.nome
            ORDER BY SUM(a.valor) DESC
            """)
    List<Object[]> rankingServicos();
}
