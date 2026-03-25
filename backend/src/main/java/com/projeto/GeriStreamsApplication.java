package com.projeto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal da aplicação GeriStreams.
 *
 * Esta é a classe de entrada (entry point) de toda a aplicação Spring Boot.
 * Quando o Java inicializa o programa, ele procura pelo método main(String[] args)
 * e é aqui que tudo começa.
 *
 * A anotação @SpringBootApplication é uma "super-anotação" que combina três anotações em uma só,
 * simplificando a configuração:
 *
 *   1. @Configuration
 *      → Indica que esta classe pode conter definições de beans Spring (@Bean).
 *        Um "bean" é qualquer objeto gerenciado pelo contêiner IoC (Inversão de Controle) do Spring.
 *
 *   2. @EnableAutoConfiguration
 *      → Pede ao Spring para configurar automaticamente os componentes da aplicação
 *        com base nas dependências presentes no pom.xml (classpath).
 *        Exemplo: se detectar spring-data-jpa no classpath, configura automaticamente
 *        o EntityManager, DataSource, etc.
 *        Exemplo: se detectar spring-web, configura automaticamente o servidor Tomcat embutido.
 *
 *   3. @ComponentScan
 *      → Diz ao Spring para varrer este pacote (com.projeto) e todos os subpacotes,
 *        procurando por classes anotadas com @Component, @Service, @Repository,
 *        @Controller, @RestController, etc., e registrando-as no contexto do Spring.
 *        Sem isso, o Spring não "saberia" que suas classes existem.
 */
@SpringBootApplication
public class GeriStreamsApplication {

    /**
     * Método principal — ponto de entrada da JVM (Java Virtual Machine).
     *
     * @param args Argumentos de linha de comando (raramente usados diretamente).
     *
     * O que SpringApplication.run() faz internamente:
     *   1. Cria o contexto da aplicação Spring (ApplicationContext), que é o "contêiner"
     *      que gerencia todos os beans (objetos) da aplicação.
     *   2. Executa o @ComponentScan, detectando todos os @Service, @Repository, etc.
     *   3. Executa a auto-configuração (banco de dados, segurança, etc.).
     *   4. Inicia o servidor Tomcat embutido na porta definida em application.properties
     *      (padrão: 8080).
     *   5. A aplicação fica "de pé" esperando requisições HTTP.
     */
    public static void main(String[] args) {
        SpringApplication.run(GeriStreamsApplication.class, args);
    }
}
