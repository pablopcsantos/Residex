# Guia de implantação de uma instância do Residex

Este documento ensina a preparar a infraestrutura que fornece dados ao aplicativo: uma planilha no Google Drive, uma API no Google Apps Script e a configuração do app Android.

## Visão geral

~~~text
Planilha Google no Drive
          ↕
Google Apps Script publicado como aplicativo web
          ↕ HTTPS/JSON
Aplicativo Android
          ↕
Banco local Room
~~~

A planilha é a fonte principal. O Apps Script acessa essa planilha em nome do proprietário, publica os registros ativos e processa operações administrativas. O app nunca recebe permissão direta para acessar o Google Drive.

## Arquivos fornecidos

- `reference/data/CalendarioResidencias-GoogleSheets-v1.3.xlsx`: modelo para criar a planilha no Google Drive;
- `reference/apps-script/Code.gs`: código da API que lê e altera a planilha.

O arquivo Excel contém somente as abas `LEIA-ME`, `SELEÇÕES` e `CONFIG`. A primeira documenta o modelo para quem administra a planilha; `SELEÇÕES` armazena os registros consumidos pelo aplicativo; e `CONFIG` mantém as configurações utilizadas pelo Apps Script.

~~~text
Planilha de referência
├── LEIA-ME
├── SELEÇÕES
└── CONFIG
~~~

## Pré-requisitos

- uma conta Google com acesso ao Drive, Planilhas e Apps Script;
- permissão para criar e implantar um aplicativo web no Apps Script;
- uma cópia local deste repositório;
- ambiente Android configurado conforme o README.

## 1. Criar a planilha no Google Drive

1. Acesse o Google Drive e escolha **Novo > Upload de arquivo**.
2. Envie `reference/data/CalendarioResidencias-GoogleSheets-v1.3.xlsx`.
3. Abra o arquivo com o Google Planilhas.
4. Se necessário, use **Arquivo > Salvar como Google Planilhas** para criar uma planilha nativa.
5. Confirme que a aba `SELEÇÕES` preservou os cabeçalhos.

Os cabeçalhos reconhecidos pelo app são:

~~~text
ID
UF
SELEÇÃO
EDITAL_INFORMAÇÃO
EDITAL_LINK
INSCRIÇÕES
TAXA
PROVA_OBJETIVA
ANÁLISE_CURRICULAR
PROVA_PRÁTICA
ENTREVISTA
RESULTADO_FINAL
INFORMAÇÕES_LINK
ATIVA
OBSERVAÇÕES
~~~

Não renomeie a aba `SELEÇÕES` nem esses cabeçalhos. Colunas adicionais não são consumidas pelo aplicativo e devem ser removidas do modelo para evitar ambiguidades.

Cada registro deve possuir um `ID` exclusivo, preferencialmente no formato `SEL-001`. Novos registros criados pela área administrativa recebem o próximo ID automaticamente. IDs vazios ou repetidos devem ser corrigidos antes da publicação, pois edição e exclusão são localizadas por ID.

Copie o ID da planilha na URL:

~~~text
https://docs.google.com/spreadsheets/d/ID_DA_PLANILHA/edit
~~~

## 2. Criar o projeto no Google Apps Script

1. Acesse `https://script.google.com` e crie um novo projeto.
2. Abra o arquivo padrão `Code.gs` no editor.
3. Substitua seu conteúdo pelo arquivo `reference/apps-script/Code.gs` deste repositório.
4. Altere `SPREADSHEET_ID`, no início do script, para o ID copiado no passo anterior.
5. Salve o projeto com um nome identificável, por exemplo `Residex API`.

O projeto pode ser independente da planilha porque usa `SpreadsheetApp.openById()`. A conta que executa o script precisa ter acesso de edição à planilha.

## 3. Configurar a senha administrativa

O app não armazena a senha administrativa. A aba `CONFIG` guarda somente seu hash SHA-256.

1. No fim do `Code.gs`, localize `configurarSenhaAdmin()`.
2. Preencha temporariamente `ADMIN_PASSWORD_FOR_SETUP` com uma senha forte.
3. Salve e selecione `configurarSenhaAdmin` na lista de funções do editor.
4. Clique em **Executar** e conclua a autorização solicitada pelo Google.
5. Confirme que a aba `CONFIG` contém `APP_NAME`, `APP_DESCRIPTION` e `ADMIN_HASH`.
6. Apague imediatamente a senha da constante, deixando novamente uma string vazia, e salve o script.

Nunca faça commit do script enquanto a senha temporária estiver preenchida. Ao trocar a senha, repita o procedimento e remova novamente o valor em texto puro.

## 4. Implantar a API como aplicativo web

1. No editor do Apps Script, escolha **Implantar > Nova implantação**.
2. Em **Selecionar tipo**, escolha **Aplicativo da Web**.
3. Configure a execução como o proprietário do projeto.
4. Escolha quem poderá acessar a implantação de acordo com a política da conta. Para que o app distribuído consulte os dados sem login Google, a URL precisa aceitar acesso público.
5. Autorize o projeto e conclua a implantação.
6. Copie a URL terminada em `/exec`. Essa é a URL da API usada pelo aplicativo.

Uma alteração futura no `Code.gs` exige uma nova versão da implantação em **Gerenciar implantações**. Apenas salvar o código não atualiza necessariamente a versão já publicada.

## 5. Testar a API

Abra estas URLs no navegador, substituindo `URL_DO_APPS_SCRIPT`:

~~~text
URL_DO_APPS_SCRIPT?resource=health
URL_DO_APPS_SCRIPT?resource=data
~~~

O endpoint `health` deve retornar `"ok": true`. O endpoint `data` deve retornar um objeto JSON com a lista `selecoes`. Na leitura pública, registros com `ATIVA` igual a `FALSE` não são enviados.

As operações administrativas usam requisições `POST` com as ações `authenticate`, `getAdminData`, `saveSelection` e `deleteSelection`. Elas são executadas pelo próprio aplicativo e exigem a senha configurada.

## 6. Conectar o aplicativo Android

Há duas maneiras de informar a URL:

- durante o desenvolvimento, altere `RESIDENCY_API_URL` em `app/src/main/java/com/pablopcsantos/residex/utils/Constants.kt`;
- em um app já instalado, use a tela **Ajustes** para substituir e testar a URL.

Depois de configurar a URL:

1. faça um teste de conexão em **Ajustes**;
2. execute uma sincronização manual;
3. confirme que as seleções aparecem no calendário;
4. entre em **Administração** e teste a senha;
5. crie um registro de teste, confira a planilha e depois remova o registro.

Após uma resposta válida, o app converte o JSON com Retrofit/Moshi e grava uma cópia local no Room. Essa cópia alimenta as telas e permite manter os últimos dados disponíveis quando não há conexão.

## Segurança e manutenção

- compartilhe a planilha somente com as contas que realmente precisam editá-la;
- não publique senha, hash, ID privado, token, keystore ou credenciais no Git;
- trate a URL pública como um endpoint acessível por terceiros;
- use HTTPS, já fornecido pela implantação do Apps Script;
- mantenha IDs únicos e faça backup periódico da planilha;
- valide os dados antes de cada publicação;
- guarde uma cópia segura da planilha e do `Code.gs` implantado;
- se a URL da implantação mudar, atualize a configuração do app.

## Solução de problemas

- **A aba SELEÇÕES não foi encontrada:** confira exatamente o nome da aba.
- **Não foi possível abrir a planilha:** valide `SPREADSHEET_ID` e as permissões da conta que executa o script.
- **Senha inválida:** execute novamente `configurarSenhaAdmin()` e confira `ADMIN_HASH` na aba `CONFIG`.
- **O navegador mostra código antigo:** crie uma nova versão da implantação.
- **O app recebe lista vazia:** confirme que há linhas preenchidas e que `ATIVA` não está definida como `FALSE` para todos os registros.
- **Edição ou exclusão atinge o item errado:** procure IDs vazios ou duplicados na aba `SELEÇÕES`.
