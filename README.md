# 🛡️ Esteira DevSecOps Automatizada — Java 21 & Spring Boot 3

Repositório de implementação prática de uma **Pipeline CI/CD DevSecOps de ponta a ponta** integrada ao **GitHub Actions**, cobrindo desde o desenvolvimento de uma API REST em **Java 21 (Spring Boot 3)** até o empacotamento seguro com **Docker**, **Infraestrutura como Código (Kubernetes)** e **Análise Dinâmica (DAST)**.

---

## 🏗️ 1. Arquitetura da Pipeline e Ferramentas Escolhidas

A pipeline está definida em [`.github/workflows/devsecops.yml`](.github/workflows/devsecops.yml) e é acionada automaticamente a cada `push` ou `pull_request` na branch `main`.

| Etapa | Requisito Atendido | Ferramenta | O que faz | Security Gate (Bloqueio) |
| :--- | :--- | :--- | :--- | :--- |
| **1. Secret Scanning** | Varredura de Segredos | **Gitleaks** | Analisa o código-fonte em busca de chaves privadas, senhas, tokens JWT ou credenciais da AWS hardcoded. | `--exit-code 1` (bloqueia a esteira imediatamente se encontrar 1 segredo). |
| **2. SAST & Testes** | Análise Estática do Código | **Semgrep** + **JUnit/MockMvc** | Valida as regras de negócio da API e analisa o código Java/Spring em busca de SQL Injection, XSS, CORS inseguro e OWASP Top 10. | `--error --severity ERROR` (interrompe o build se detectar código vulnerável). |
| **3. SCA & SBOM** | Segurança na Cadeia de Suprimentos | **Trivy FS** (`cyclonedx`) | Analisa todas as bibliotecas do `pom.xml` em busca de CVEs conhecidas e gera o inventário **SBOM** no formato **CycloneDX** (`sbom-cyclonedx.json`). | `exit-code: '1'` para vulnerabilidades `HIGH` ou `CRITICAL`. |
| **4. Containers & IaC** | Segurança de Container e Infraestrutura | **Checkov** + **Trivy Image** | Valida se o `Dockerfile` usa usuário não-root (`USER appuser`) e `HEALTHCHECK`, analisa o manifesto Kubernetes (`k8s/deployment.yaml`) e escaneia a imagem Docker construída. | Bloqueia se o container rodar como `root`, se faltar limite de CPU/Memória no K8s ou se houver CVE crítica na imagem. |
| **5. DAST** | Análise Dinâmica de Segurança | **Container Staging + cURL + Nuclei** | Sobe o container da API na pipeline, aguarda o `/api/health` e dispara requisições HTTP reais testando cabeçalhos de segurança, validação de entrada e SQL Injection. | `exit 1` se faltar qualquer cabeçalho HTTP de proteção ou se o Nuclei achar falha grave. |

---

## 📂 2. Estrutura do Repositório

```text
├── .github/workflows/
│   └── devsecops.yml               # Definição completa da pipeline CI/CD DevSecOps
├── k8s/
│   └── deployment.yaml             # Manifesto Kubernetes (IaC) endurecido (Non-Root, ReadOnlyFS, Limits)
├── src/
│   ├── main/java/com/devsecops/api/
│   │   ├── DevSecOpsApplication.java       # Classe principal Spring Boot
│   │   ├── config/SecurityHeadersFilter.java # Filtro HTTP (CSP, X-Frame-Options, nosniff)
│   │   ├── controller/ItemController.java  # Endpoints REST (/api/health e /api/items)
│   │   ├── model/Item.java                 # Entidade JPA com Bean Validation (@NotBlank, @Size)
│   │   └── repository/ItemRepository.java  # Queries parametrizadas (proteção contra SQL Injection)
│   ├── main/resources/
│   │   ├── application.properties          # Configuração segura (stacktrace oculto, H2 console off)
│   │   └── data.sql                        # Carga inicial do banco em memória
│   └── test/java/com/devsecops/api/
│       └── ItemControllerTest.java         # Testes automatizados de endpoints e Security Headers
├── Dockerfile                      # Multi-stage build com Alpine JRE 21 e usuário não-root (appuser)
├── docker-compose.yml              # Orquestração local com read_only e cap_drop: ALL
└── pom.xml                         # Manifesto Maven (Java 21 + Spring Boot 3.4.3)
```

---

## 🎤 3. Roteiro Prático para Apresentação em Sala de Aula (10 a 15 min)

### Passo 1: Visão Geral do Código e da Pipeline (2 min)
1. Mostre rapidamente a estrutura do repositório:
   - A API em [`ItemController.java`](src/main/java/com/devsecops/api/controller/ItemController.java) e o filtro [`SecurityHeadersFilter.java`](src/main/java/com/devsecops/api/config/SecurityHeadersFilter.java).
   - O [`Dockerfile`](Dockerfile) destacando o **Multi-stage build** e a linha `USER appuser` (não roda como `root`).
   - O manifesto [`k8s/deployment.yaml`](k8s/deployment.yaml) destacando `runAsNonRoot: true`, `readOnlyRootFilesystem: true` e limites de CPU/Memória.
   - O arquivo [`.github/workflows/devsecops.yml`](.github/workflows/devsecops.yml) mostrando os 5 jobs encadeados.

---

### Passo 2: Demonstração ao Vivo do Bloqueio / Security Gate (5 min)
Você deve introduzir **uma falha intencional**, fazer `git push` e mostrar a pipeline **bloqueando (Build Failed - Vermelho)**.

Escolha **uma** das opções abaixo durante a aula:

#### 🔴 Opção A (Mais Rápida — Bloqueia em ~25s na Etapa 1: Secret Scanning)
Adicione uma chave da AWS ou token simulado em `src/main/resources/application.properties`:
```properties
# Falha intencional para demonstrar o Security Gate do Gitleaks:
aws.secret.access.key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
aws.access.key.id=AKIAIOSFODNN7EXAMPLE
```
No terminal, execute:
```bash
git add src/main/resources/application.properties
git commit -m "test: demonstracao de bloqueio secret scanning"
git push
```
> **O que mostrar na tela:** Abra a aba **Actions** no GitHub. O job **1. Secret Scanning (Gitleaks)** vai detectar a chave `AKIAIOSFODNN7EXAMPLE`, retornar `exit-code 1` e **travar toda a esteira**, impedindo que os próximos estágios rodem.

#### 🔴 Opção B (Bloqueio na Etapa 4: Container / IaC rodando como `root`)
No arquivo `Dockerfile`, comente ou apague a linha `USER appuser` (linha 31) ou troque por `USER root`.
```bash
git add Dockerfile
git commit -m "test: container rodando como root"
git push
```
> **O que mostrar na tela:** A etapa **4. Container & IaC Security** vai reprovar imediatamente acusando execução como `root` tanto no verificador quanto no **Checkov (`CKV_DOCKER_3`)**.

---

### Passo 3: Demonstração do Fluxo Seguro e Artefato SBOM (5 min)
1. Remova a linha insegura que você adicionou no Passo 2 e envie a correção:
```bash
git revert HEAD --no-edit
git push
```
2. Mostre na aba **Actions** do GitHub todos os **5 estágios ficando verdes (Build Passed)**:
   - `1. Secret Scanning (Gitleaks)` ✅
   - `2. SAST & Unit Tests (Semgrep + Maven)` ✅
   - `3. SCA & SBOM (Trivy + CycloneDX)` ✅
   - `4. Container & IaC Security (Checkov + Trivy Image)` ✅
   - `5. DAST - Analise Dinamica da API (Live Container + Nuclei)` ✅
3. Role até o final da página do resumo da execução no GitHub Actions (seção **Artifacts**) e clique em **`sbom-cyclonedx-artifact`** para mostrar o inventário de software (`sbom-cyclonedx.json`) gerado no padrão **CycloneDX**.

---

### Passo 4: Respostas Rápidas para a Arguição do Professor (3 min)

- **1. "Qual a diferença entre SAST, SCA e DAST na sua esteira?"**
  - **SAST (Semgrep):** Analisa o *nosso código-fonte* Java sem executá-lo (caixa-branca), buscando erros de programação como SQL Injection ou falta de sanitização.
  - **SCA (Trivy FS):** Analisa as *bibliotecas de terceiros* declaradas no `pom.xml` (Spring, H2, Jackson) contra bancos de vulnerabilidades públicas (CVEs) e gera o inventário **SBOM (CycloneDX)**.
  - **DAST (cURL + Nuclei):** Testa a *aplicação rodando de verdade* dentro do container (caixa-preta), fazendo chamadas HTTP contra a porta `8080` para checar cabeçalhos de segurança e respostas a payloads maliciosos.
- **2. "Como o seu container e o seu Kubernetes evitam ataques de escalação de privilégio?"**
  - No `Dockerfile`, criamos um usuário sem privilégios (`appuser`) e usamos a diretiva `USER appuser`. No Kubernetes (`k8s/deployment.yaml`), forçamos `runAsNonRoot: true`, `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem: true` e removemos todas as capabilities do kernel Linux (`drop: ["ALL"]`).
- **3. "O que é um Security Gate e como ele foi configurado?"**
  - É uma barreira automatizada na pipeline que impede que código vulnerável avance para produção. Configuramos todas as ferramentas com código de saída de erro (`--exit-code 1` no Gitleaks e Trivy, `--error` no Semgrep) e encadeamos os jobs com `needs:` no GitHub Actions. Se uma etapa falha, o deploy é abortado automaticamente.
