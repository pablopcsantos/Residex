# Residex

Residex é um aplicativo Android para acompanhar processos seletivos de
residência médica. O app reúne inscrições, provas, resultados e links oficiais
em uma experiência otimizada para smartphones, com dados sincronizados e cache
local.

## 👤 Autoria e desenvolvimento

O Residex é um aplicativo Android nativo desenvolvido de forma independente por Pablo Phillipe Cândido dos Santos, destinado a apoiar candidatos no acompanhamento de processos seletivos de residência médica. Reúne calendários, prazos, etapas e links de editais e informações, com pesquisa, filtros, notificações configuráveis e disponibilidade local dos dados sincronizados.

O desenvolvimento contou com a utilização de ferramentas de inteligência artificial generativa como recurso auxiliar, mantendo-se sob responsabilidade do autor a concepção, implementação, integração e verificação do projeto.

Currículo Lattes: [http://lattes.cnpq.br/9500873674712528](http://lattes.cnpq.br/9500873674712528)

## Funcionalidades

- calendário pesquisável com filtros por UF e situação;
- filtros independentes para inscrições e provas nos próximos sete dias;
- filtros e ordenação em um menu lateral, preservando espaço para os resultados;
- seleções acompanhadas com ordem manual persistente;
- detalhes de inscrições, taxas, etapas, resultados, editais e links oficiais;
- sincronização manual e periódica com a API configurada;
- administração autenticada para incluir, editar e excluir seleções;
- notificações locais configuráveis, com prevenção de duplicidade;
- solicitação de permissão de notificações no Android 13 ou superior;
- botão para enviar uma notificação de teste;
- temas claro e escuro com a identidade visual do Residex;
- cache Room para manter os dados disponíveis entre sincronizações.

## Navegação

| Tela | Finalidade |
| --- | --- |
| Calendário | Consultar as seleções acompanhadas, pesquisar, filtrar e ordenar |
| Seleções | Escolher processos acompanhados e configurar a ordem manual |
| Administração | Autenticar e gerenciar os dados publicados |
| Ajustes | Configurar conexão, sincronização e notificações |

Os filtros de UF, situação e ordenação ficam no menu lateral da tela de
calendário. O ícone informa quantos filtros estão ativos e a tela principal
permanece dedicada à busca e aos resultados.

## Tecnologia e requisitos

O projeto utiliza Kotlin, Jetpack Compose, Material 3, Navigation Compose,
Hilt, Retrofit/Moshi, Room e WorkManager.

- Java 17;
- Android SDK 36;
- Android Build Tools 36.0.0;
- SDK mínimo: Android 7.1, API 25;
- SDK alvo: API 36.

O suporte às APIs modernas de data em aparelhos antigos usa core library
desugaring.

## Fonte de dados e configuração

Os dados publicados pelo Residex têm como fonte uma planilha do Google
Planilhas armazenada no Google Drive. Essa planilha funciona como o banco de
dados administrado do projeto, mas o aplicativo Android não a acessa
diretamente. Um projeto do Google Apps Script, implantado como aplicativo web,
faz a intermediação: lê e altera a planilha e expõe os resultados ao app em
formato JSON.

O trânsito dos dados ocorre da seguinte forma:

1. os processos seletivos são mantidos na aba `SELEÇÕES` da planilha no Google
   Drive;
2. o Google Apps Script lê as linhas da planilha e as transforma em objetos
   JSON;
3. o aplicativo consulta a URL pública do Apps Script usando Retrofit e Moshi;
4. após uma sincronização válida, o app grava os dados em um banco Room no
   aparelho;
5. as telas consultam essa cópia local, que continua disponível sem conexão ou
   quando uma sincronização falha.

Na área administrativa, o caminho também funciona no sentido inverso. Depois
da autenticação, inclusões, alterações e exclusões são enviadas ao Apps Script
por requisições `POST`. O script valida a senha administrativa e, quando a
operação é autorizada, modifica a planilha no Google Drive e devolve ao app a
lista atualizada. A senha não dá ao aplicativo acesso direto ao Google Drive e
não é armazenada no aparelho.

~~~text
Google Planilhas (Google Drive)
             ↕
Google Apps Script (API JSON)
             ↕
Aplicativo Android (Retrofit/Moshi)
             ↕
Banco local Room e telas do app
~~~

O aplicativo inclui uma URL pública padrão para essa API do Google Apps
Script. Ela pode ser alterada e testada na tela **Ajustes**. Uma implantação em
outro ambiente precisa apontar o Apps Script para a planilha correta e informar
ao app a URL correspondente.

O formato geral retornado na leitura pública é:

~~~json
{
  "ok": true,
  "selecoes": [
    {
      "ID": "SEL-001",
      "UF": "SP",
      "SELEÇÃO": "Nome da instituição",
      "ATIVA": "TRUE"
    }
  ],
  "generatedAt": "2026-08-19T00:00:00.000Z"
}
~~~

Para criar uma instância completa, desde a importação da planilha no Google
Drive até a implantação do Apps Script e a configuração do app, consulte o
[Guia de implantação](docs/GUIA_DE_IMPLANTACAO.md).

## Segurança e privacidade

- a URL pública da API não contém credenciais administrativas;
- a senha administrativa é enviada apenas nas operações autenticadas e não é
  persistida pelo aplicativo;
- preferências de acompanhamento, ordenação e notificações permanecem no
  aparelho;
- o projeto não inclui publicidade nem bibliotecas de analytics;
- senhas, tokens, keystores e arquivos de ambiente não devem ser adicionados ao
  repositório.

O material em reference/apps-script/ contém uma rotina auxiliar de configuração
de senha. A constante temporária deve permanecer vazia no código versionado.

## Build local

O Codespace pode instalar o SDK automaticamente usando
.devcontainer/devcontainer.json e scripts/setup-android-sdk.sh.

Para ambientes já configurados, execute as tarefas separadamente para reduzir
o pico de memória:

~~~bash
./gradlew --no-daemon --max-workers=1 test
./gradlew --no-daemon --max-workers=1 lintDebug
./gradlew --no-daemon --max-workers=1 assembleDebug
~~~

O APK de desenvolvimento é gerado em:

~~~text
app/build/outputs/apk/debug/app-debug.apk
~~~

## Testes e verificações

Os testes unitários cobrem regras de datas e situações, sincronização do
repositório, notificações e ordenação das seleções. A tarefa test executa as
variantes debug e release.

Antes de enviar alterações, recomenda-se executar:

~~~bash
./gradlew --no-daemon --max-workers=1 test lintDebug assembleDebug
~~~

Em ambientes com pouca memória, prefira os três comandos separados mostrados
na seção anterior. Testes instrumentados em aparelho ainda não fazem parte do
repositório.

## Estrutura relevante

~~~text
app/src/main/java/com/pablopcsantos/residex/
├── navigation/             Navegação principal do aplicativo
├── residency/data/         API, Room, preferências e repositórios
├── residency/domain/       Modelos e regras de seleção
├── residency/notification/ Canais, preferências e regras de notificação
├── residency/ui/           Telas e ViewModels do produto
├── residency/work/         Sincronização e notificações periódicas
└── ui/theme/               Paleta, tipografia e tema Material 3

app/src/test/               Testes unitários
reference/                  Fontes web e documentos usados como referência
~~~

O diretório reference/ não é empacotado no APK. Ele contém o Apps Script do backend e uma planilha de dados de referência.

## Identidade Android

- nome exibido: Residex;
- namespace e application ID: com.pablopcsantos.residex;
- classe Application: ResidexApp;
- tema Android: Theme.Residex;
- versão atual do projeto: 3.2.4 (versionCode 3).

