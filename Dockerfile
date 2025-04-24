# ---- Estágio 1: Build ----
# Usamos uma imagem oficial do Gradle que já contém o JDK 17
FROM gradle:8.8.0-jdk17 AS build

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia os arquivos de build do Gradle para aproveitar o cache do Docker
COPY build.gradle settings.gradle gradlew gradlew.bat /app/
COPY gradle /app/gradle

# Garante permissão de execução para o gradlew dentro do container
RUN chmod +x ./gradlew

# Copia o código fonte da sua aplicação
COPY src /app/src

# Executa o comando do Gradle para construir o JAR executável (pulando testes)
# Isso acontecerá DENTRO do container Docker durante o build no Render
RUN ./gradlew bootJar -x test

# ---- Estágio 2: Runtime ----
# Usamos uma imagem base leve, apenas com o Java Runtime Environment (JRE) 17
# Escolhemos a imagem Temurin (sucessor do AdoptOpenJDK) baseada no Ubuntu Jammy
FROM eclipse-temurin:17-jre-jammy

# Define o diretório de trabalho
WORKDIR /app

# Copia APENAS o arquivo JAR construído no estágio anterior para a imagem final
# O nome do JAR pode variar, o *.jar pega o que foi gerado
COPY --from=build /app/build/libs/*.jar app.jar

# Expõe a porta que a aplicação Spring Boot vai usar
# Usamos 8080 como padrão do Spring Boot, ou o valor de ${PORT:XXXX} do application.properties
# O Render vai mapear a porta pública para esta porta exposta.
EXPOSE 8080
# Ou EXPOSE 9090 se você manteve essa porta no application.properties

# Comando que será executado quando o container iniciar no Render
ENTRYPOINT ["java", "-jar", "/app/app.jar"]