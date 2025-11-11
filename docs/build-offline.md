# Build offline com Maven

Alguns ambientes corporativos bloqueiam downloads diretos do Maven Central, retornando erro HTTP 403. Utilize um mirror local ou repositório proxy para montar o build offline.

## Opção 1 — `settings.xml` com mirror público confiável
```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>ufs-maven-mirror</id>
      <url>https://repo1.maven.org/maven2</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```
Salve o arquivo em `~/.m2/settings.xml` (Linux/macOS) ou `%USERPROFILE%\.m2\settings.xml` (Windows).

## Opção 2 — Preparar dependências offline
1. Configure o mirror acima ou um Nexus/Artifactory interno.
2. Execute `mvn -q dependency:go-offline` para baixar todas as dependências transitivas.
3. Copie o diretório `~/.m2/repository` para as máquinas que ficarão offline.
4. Rode `mvn -o clean package` (flag `-o` força o modo offline).

## Dependências utilizadas
- `com.google.code.gson:gson:2.10.1`
- `org.junit.jupiter:junit-jupiter:5.10.2`

Certifique-se de sincronizar essas versões antes de desconectar da internet.
