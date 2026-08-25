/**
 * ============================================================
 * CALENDÁRIO DE RESIDÊNCIAS
 * Google Apps Script - API
 * ============================================================
 *
 * Arquitetura:
 *
 * Google Sheets
 *      ↓
 * Google Apps Script
 *      ↓
 * Aplicativo Android
 *
 * O Apps Script funciona como API para:
 *
 * GET:
 *   - fornecer os dados das seleções
 *   - verificar se a API está funcionando
 *
 * POST:
 *   - autenticar administrador
 *   - obter dados administrativos
 *   - salvar seleção
 *   - excluir seleção
 *
 * Não há integração com Telegram.
 * Não há interface HTML administrativa.
 * Não utiliza SpreadsheetApp.getUi().
 */


/**
 * ============================================================
 * CONFIGURAÇÃO PRINCIPAL
 * ============================================================
 *
 * ID da Google Planilhas utilizada como banco de dados.
 */
const SPREADSHEET_ID =
  '16gTxhaabygcRmPDL06EXP8jz5UMYJivmGK80mCNQrd0';


/**
 * ============================================================
 * ACESSO À PLANILHA
 * ============================================================
 *
 * Abre explicitamente a planilha pelo ID.
 *
 * Isso evita depender de:
 *
 * SpreadsheetApp.getActiveSpreadsheet()
 *
 * que pode retornar null quando o projeto é executado
 * fora do contexto direto da planilha.
 */
function getSpreadsheet_() {
  if (!SPREADSHEET_ID) {
    throw new Error(
      'SPREADSHEET_ID não foi configurado no Code.gs.'
    );
  }

  try {
    return SpreadsheetApp.openById(
      SPREADSHEET_ID
    );
  } catch (error) {
    throw new Error(
      'Não foi possível abrir a planilha pelo SPREADSHEET_ID. ' +
      'Verifique se o ID está correto e se o Apps Script possui acesso à planilha. ' +
      'Detalhes: ' +
      (error.message || String(error))
    );
  }
}


/**
 * ============================================================
 * GET
 * ============================================================
 *
 * Endpoint utilizado pelo aplicativo Android.
 *
 * Exemplos:
 *
 * /exec?resource=data
 * /exec?resource=health
 */
function doGet(e) {
  try {
    const resource =
      e &&
      e.parameter &&
      e.parameter.resource
        ? String(e.parameter.resource).trim()
        : '';


    /**
     * --------------------------------------------------------
     * Endpoint de dados
     * --------------------------------------------------------
     */
    if (resource === 'data') {
      return jsonOutput_({
        ok: true,
        ...getAppData()
      });
    }


    /**
     * --------------------------------------------------------
     * Endpoint de saúde
     * --------------------------------------------------------
     */
    if (resource === 'health') {
      return jsonOutput_({
        ok: true,
        service:
          'Calendário de Residências API',
        timestamp:
          new Date().toISOString()
      });
    }


    /**
     * --------------------------------------------------------
     * Resposta padrão
     * --------------------------------------------------------
     */
    return jsonOutput_({
      ok: true,
      service:
        'Calendário de Residências API',
      message:
        'API funcionando corretamente.',
      timestamp:
        new Date().toISOString()
    });

  } catch (error) {
    return jsonOutput_({
      ok: false,
      error:
        error.message ||
        String(error)
    });
  }
}


/**
 * ============================================================
 * POST
 * ============================================================
 *
 * Ações disponíveis:
 *
 * authenticate
 * getAdminData
 * saveSelection
 * deleteSelection
 */
function doPost(e) {
  try {
    if (
      !e ||
      !e.postData ||
      !e.postData.contents
    ) {
      return jsonOutput_({
        ok: false,
        error:
          'Nenhum conteúdo POST foi recebido.'
      });
    }

    const body =
      JSON.parse(
        e.postData.contents
      );

    const action =
      String(
        body.action || ''
      ).trim();


    /**
     * --------------------------------------------------------
     * AUTENTICAÇÃO ADMINISTRATIVA
     * --------------------------------------------------------
     */
    if (action === 'authenticate') {
      const password =
        String(
          body.password || ''
        );

      const authenticated =
        authenticateAdmin(
          password
        );

      return jsonOutput_({
        ok: true,
        authenticated:
          authenticated
      });
    }


    /**
     * --------------------------------------------------------
     * DADOS ADMINISTRATIVOS
     * --------------------------------------------------------
     */
    if (action === 'getAdminData') {
      const password =
        String(
          body.password || ''
        );

      const data =
        getAdminData(
          password
        );

      return jsonOutput_({
        ok: true,
        data: data
      });
    }


    /**
     * --------------------------------------------------------
     * SALVAR SELEÇÃO
     * --------------------------------------------------------
     */
    if (action === 'saveSelection') {
      const password =
        String(
          body.password || ''
        );

      const item =
        body.item &&
        typeof body.item === 'object'
          ? body.item
          : {};

      const result =
        saveSelection(
          password,
          item
        );

      return jsonOutput_({
        ok: true,
        data: result
      });
    }


    /**
     * --------------------------------------------------------
     * EXCLUIR SELEÇÃO
     * --------------------------------------------------------
     */
    if (action === 'deleteSelection') {
      const password =
        String(
          body.password || ''
        );

      const id =
        String(
          body.id || ''
        ).trim();

      const result =
        deleteSelection(
          password,
          id
        );

      return jsonOutput_({
        ok: true,
        data: result
      });
    }


    /**
     * --------------------------------------------------------
     * AÇÃO DESCONHECIDA
     * --------------------------------------------------------
     */
    return jsonOutput_({
      ok: false,
      error:
        'Ação não reconhecida: ' +
        action
    });

  } catch (error) {
    return jsonOutput_({
      ok: false,
      error:
        error.message ||
        String(error)
    });
  }
}


/**
 * ============================================================
 * DADOS PÚBLICOS
 * ============================================================
 *
 * Lê a aba SELEÇÕES.
 *
 * Retorna apenas registros ATIVOS quando a coluna ATIVA existe.
 */
function getAppData() {
  const spreadsheet =
    getSpreadsheet_();

  const sheet =
    spreadsheet.getSheetByName(
      'SELEÇÕES'
    );

  if (!sheet) {
    throw new Error(
      'A aba "SELEÇÕES" não foi encontrada.'
    );
  }

  if (
    sheet.getLastRow() < 2 ||
    sheet.getLastColumn() < 1
  ) {
    return {
      selecoes: [],
      generatedAt:
        new Date().toISOString()
    };
  }

  /**
   * getDisplayValues() preserva a aparência
   * dos dados na planilha.
   */
  const values =
    sheet
      .getDataRange()
      .getDisplayValues();

  const headers =
    values.shift();

  const selecoes =
    values
      .filter(function(row) {
        return row.some(function(value) {
          return (
            String(value)
              .trim() !== ''
          );
        });
      })
      .map(function(row) {
        return objectFromRow_(
          headers,
          row
        );
      })
      .filter(function(item) {

        /**
         * Se existir ATIVA:
         * FALSE significa que não deve
         * aparecer para os usuários.
         */
        if (
          Object.prototype.hasOwnProperty.call(
            item,
            'ATIVA'
          )
        ) {
          return (
            String(item.ATIVA)
              .trim()
              .toUpperCase() !== 'FALSE'
          );
        }

        /**
         * Se a coluna não existir,
         * consideramos o registro ativo.
         */
        return true;
      });

  return {
    selecoes:
      selecoes,
    generatedAt:
      new Date().toISOString()
  };
}


/**
 * ============================================================
 * DADOS ADMINISTRATIVOS
 * ============================================================
 *
 * Retorna todos os registros, inclusive inativos,
 * depois de autenticar o administrador.
 */
function getAdminData(password) {
  assertAdmin_(
    password
  );

  const spreadsheet =
    getSpreadsheet_();

  const sheet =
    spreadsheet.getSheetByName(
      'SELEÇÕES'
    );

  if (!sheet) {
    throw new Error(
      'A aba "SELEÇÕES" não foi encontrada.'
    );
  }

  if (
    sheet.getLastColumn() < 1
  ) {
    return {
      headers: [],
      selecoes: [],
      generatedAt:
        new Date().toISOString()
    };
  }

  const values =
    sheet
      .getDataRange()
      .getDisplayValues();

  if (
    values.length === 0
  ) {
    return {
      headers: [],
      selecoes: [],
      generatedAt:
        new Date().toISOString()
    };
  }

  const headers =
    values.shift();

  const selecoes =
    values
      .filter(function(row) {
        return row.some(function(value) {
          return (
            String(value)
              .trim() !== ''
          );
        });
      })
      .map(function(row) {
        return objectFromRow_(
          headers,
          row
        );
      });

  return {
    headers:
      headers,
    selecoes:
      selecoes,
    generatedAt:
      new Date().toISOString()
  };
}


/**
 * ============================================================
 * AUTENTICAÇÃO ADMINISTRATIVA
 * ============================================================
 */
function authenticateAdmin(password) {
  const cleanPassword =
    String(
      password || ''
    ).trim();

  if (!cleanPassword) {
    return false;
  }

  const config =
    getConfig_();

  if (
    !config.adminHash
  ) {
    return false;
  }

  const suppliedHash =
    hashString_(
      cleanPassword
    );

  return (
    suppliedHash ===
    String(
      config.adminHash
    )
  );
}


/**
 * ============================================================
 * SALVAR / EDITAR SELEÇÃO
 * ============================================================
 */
function saveSelection(
  password,
  item
) {
  assertAdmin_(
    password
  );

  if (
    !item ||
    typeof item !== 'object'
  ) {
    throw new Error(
      'Os dados da seleção são inválidos.'
    );
  }

  const spreadsheet =
    getSpreadsheet_();

  const sheet =
    spreadsheet.getSheetByName(
      'SELEÇÕES'
    );

  if (!sheet) {
    throw new Error(
      'A aba "SELEÇÕES" não foi encontrada.'
    );
  }

  if (
    sheet.getLastColumn() < 1
  ) {
    throw new Error(
      'A aba "SELEÇÕES" não possui cabeçalhos.'
    );
  }

  const headers =
    sheet
      .getRange(
        1,
        1,
        1,
        sheet.getLastColumn()
      )
      .getDisplayValues()[0];

  const clean = {};

  /**
   * Copia apenas os campos existentes
   * no cabeçalho da planilha.
   */
  headers.forEach(
    function(header) {
      clean[header] =
        item[header] !== undefined &&
        item[header] !== null
          ? item[header]
          : '';
    }
  );


  /**
   * Gera ID automaticamente.
   */
  if (
    !String(
      clean.ID || ''
    ).trim()
  ) {
    clean.ID =
      nextSelectionId_(
        sheet
      );
  }


  /**
   * Normaliza ATIVA.
   */
  if (
    Object.prototype.hasOwnProperty.call(
      clean,
      'ATIVA'
    )
  ) {
    clean.ATIVA =
      normalizeBoolean_(
        clean.ATIVA,
        true
      );
  }


  /**
   * Verifica se é edição ou criação.
   */
  const existingRow =
    findRowById_(
      sheet,
      clean.ID
    );


  /**
   * Monta linha exatamente na ordem
   * dos cabeçalhos.
   */
  const row =
    headers.map(
      function(header) {
        return clean[header];
      }
    );


  if (existingRow) {

    sheet
      .getRange(
        existingRow,
        1,
        1,
        row.length
      )
      .setValues([
        row
      ]);

  } else {

    sheet.appendRow(
      row
    );
  }


  return {
    saved: true,
    id:
      clean.ID,
    data:
      getAppData()
  };
}


/**
 * ============================================================
 * EXCLUIR SELEÇÃO
 * ============================================================
 */
function deleteSelection(
  password,
  id
) {
  assertAdmin_(
    password
  );

  const cleanId =
    String(
      id || ''
    ).trim();

  if (!cleanId) {
    throw new Error(
      'ID da seleção não informado.'
    );
  }

  const spreadsheet =
    getSpreadsheet_();

  const sheet =
    spreadsheet.getSheetByName(
      'SELEÇÕES'
    );

  if (!sheet) {
    throw new Error(
      'A aba "SELEÇÕES" não foi encontrada.'
    );
  }

  const row =
    findRowById_(
      sheet,
      cleanId
    );

  if (!row) {
    throw new Error(
      'Seleção não encontrada: ' +
      cleanId
    );
  }

  sheet.deleteRow(
    row
  );

  return {
    deleted: true,
    id:
      cleanId,
    data:
      getAppData()
  };
}


/**
 * ============================================================
 * VERIFICAÇÃO ADMINISTRATIVA
 * ============================================================
 */
function assertAdmin_(
  password
) {
  if (
    !authenticateAdmin(
      password
    )
  ) {
    throw new Error(
      'Senha administrativa inválida.'
    );
  }
}


/**
 * ============================================================
 * TRANSFORMA LINHA EM OBJETO
 * ============================================================
 */
function objectFromRow_(
  headers,
  row
) {
  return headers.reduce(
    function(object, header, index) {

      object[header] =
        row[index] !== undefined
          ? row[index]
          : '';

      return object;
    },
    {}
  );
}


/**
 * ============================================================
 * GERA PRÓXIMO ID
 * ============================================================
 *
 * Formato:
 *
 * SEL-001
 * SEL-002
 * SEL-003
 */
function nextSelectionId_(
  sheet
) {
  if (
    !sheet ||
    sheet.getLastRow() < 2
  ) {
    return 'SEL-001';
  }

  const ids =
    sheet
      .getRange(
        2,
        1,
        sheet.getLastRow() - 1,
        1
      )
      .getDisplayValues()
      .flat();

  let max = 0;

  ids.forEach(
    function(id) {

      const match =
        String(id)
          .trim()
          .match(
            /^SEL-(\d+)$/i
          );

      if (match) {
        max =
          Math.max(
            max,
            Number(
              match[1]
            )
          );
      }
    }
  );

  return (
    'SEL-' +
    String(
      max + 1
    ).padStart(
      3,
      '0'
    )
  );
}


/**
 * ============================================================
 * LOCALIZA LINHA PELO ID
 * ============================================================
 */
function findRowById_(
  sheet,
  id
) {
  if (
    !sheet ||
    sheet.getLastRow() < 2
  ) {
    return 0;
  }

  const ids =
    sheet
      .getRange(
        2,
        1,
        sheet.getLastRow() - 1,
        1
      )
      .getDisplayValues()
      .flat();

  const index =
    ids.findIndex(
      function(value) {
        return (
          String(value)
            .trim() ===
          String(id)
            .trim()
        );
      }
    );

  return (
    index === -1
      ? 0
      : index + 2
  );
}


/**
 * ============================================================
 * CONFIGURAÇÕES
 * ============================================================
 *
 * Estrutura da aba CONFIG:
 *
 * CHAVE | VALOR
 *
 * Chaves utilizadas:
 *
 * APP_NAME
 * APP_DESCRIPTION
 * ADMIN_HASH
 */
function getConfig_() {
  const spreadsheet =
    getSpreadsheet_();

  let sheet =
    spreadsheet.getSheetByName(
      'CONFIG'
    );


  /**
   * Cria CONFIG se necessário.
   */
  if (!sheet) {

    sheet =
      spreadsheet.insertSheet(
        'CONFIG'
      );

    sheet
      .getRange(
        1,
        1,
        1,
        2
      )
      .setValues([
        [
          'CHAVE',
          'VALOR'
        ]
      ]);
  }


  const lastRow =
    sheet.getLastRow();

  const result = {};


  if (
    lastRow >= 2
  ) {

    const values =
      sheet
        .getRange(
          2,
          1,
          lastRow - 1,
          2
        )
        .getDisplayValues();

    values.forEach(
      function(row) {

        const key =
          String(
            row[0] || ''
          ).trim();

        if (!key) {
          return;
        }

        result[key] =
          String(
            row[1] || ''
          ).trim();
      }
    );
  }


  return {
    appName:
      result.APP_NAME ||
      'Calendário de Residências',

    appDescription:
      result.APP_DESCRIPTION ||
      'Acompanha processos seletivos de residência médica.',

    adminHash:
      result.ADMIN_HASH ||
      '',

    ...result
  };
}


/**
 * ============================================================
 * DEFINE VALOR NA CONFIG
 * ============================================================
 */
function setConfigValue_(
  sheet,
  key,
  value
) {
  if (!sheet) {
    throw new Error(
      'Aba CONFIG inválida.'
    );
  }

  const lastRow =
    sheet.getLastRow();


  if (
    lastRow < 2
  ) {
    sheet.appendRow([
      key,
      value
    ]);

    return;
  }


  const values =
    sheet
      .getRange(
        2,
        1,
        lastRow - 1,
        2
      )
      .getValues();


  for (
    let i = 0;
    i < values.length;
    i++
  ) {

    if (
      String(
        values[i][0]
      ).trim() === key
    ) {

      sheet
        .getRange(
          i + 2,
          2
        )
        .setValue(
          value
        );

      return;
    }
  }


  sheet.appendRow([
    key,
    value
  ]);
}


/**
 * ============================================================
 * GARANTE CHAVE DE CONFIGURAÇÃO
 * ============================================================
 */
function ensureConfigKey_(
  sheet,
  key,
  defaultValue
) {
  if (!sheet) {
    throw new Error(
      'Aba CONFIG inválida.'
    );
  }

  const spreadsheet =
    getSpreadsheet_();

  let configSheet =
    spreadsheet.getSheetByName(
      'CONFIG'
    );

  if (!configSheet) {
    configSheet =
      sheet;
  }

  const config =
    getConfig_();

  if (
    !config[key] ||
    String(
      config[key]
    ).trim() === ''
  ) {
    setConfigValue_(
      configSheet,
      key,
      defaultValue
    );
  }
}


/**
 * ============================================================
 * CONFIGURAR SENHA ADMINISTRATIVA
 * ============================================================
 *
 * IMPORTANTE:
 *
 * Esta função não usa SpreadsheetApp.getUi().
 *
 * Para configurar a senha:
 *
 * 1. Edite temporariamente a constante abaixo.
 *
 *    const ADMIN_PASSWORD_FOR_SETUP =
 *      'MinhaSenha';
 *
 * 2. Salve o projeto.
 *
 * 3. Execute configurarSenhaAdmin().
 *
 * 4. Verifique a aba CONFIG.
 *
 * 5. Apague a senha do código.
 *
 *    const ADMIN_PASSWORD_FOR_SETUP = '';
 *
 * 6. Salve novamente.
 */
function configurarSenhaAdmin() {

  /**
   * ========================================================
   * COLOQUE TEMPORARIAMENTE A SENHA AQUI
   * ========================================================
   */
  const ADMIN_PASSWORD_FOR_SETUP =
    '';


  const password =
    String(
      ADMIN_PASSWORD_FOR_SETUP || ''
    ).trim();


  if (!password) {
    throw new Error(
      'A senha ainda não foi configurada. ' +
      'Edite temporariamente a constante ' +
      'ADMIN_PASSWORD_FOR_SETUP dentro de ' +
      'configurarSenhaAdmin() e execute novamente.'
    );
  }


  const spreadsheet =
    getSpreadsheet_();


  let sheet =
    spreadsheet.getSheetByName(
      'CONFIG'
    );


  if (!sheet) {

    sheet =
      spreadsheet.insertSheet(
        'CONFIG'
      );

    sheet
      .getRange(
        1,
        1,
        1,
        2
      )
      .setValues([
        [
          'CHAVE',
          'VALOR'
        ]
      ]);
  }


  ensureConfigKey_(
    sheet,
    'APP_NAME',
    'Calendário de Residências'
  );


  ensureConfigKey_(
    sheet,
    'APP_DESCRIPTION',
    'Acompanha processos seletivos de residência médica.'
  );


  setConfigValue_(
    sheet,
    'ADMIN_HASH',
    hashString_(
      password
    )
  );


  console.log(
    'Senha administrativa configurada com sucesso.'
  );
}


/**
 * ============================================================
 * HASH SHA-256
 * ============================================================
 */
function hashString_(
  value
) {
  const bytes =
    Utilities.computeDigest(
      Utilities.DigestAlgorithm.SHA_256,
      String(value),
      Utilities.Charset.UTF_8
    );


  return bytes
    .map(
      function(byte) {

        const normalized =
          byte < 0
            ? byte + 256
            : byte;

        return normalized
          .toString(16)
          .padStart(
            2,
            '0'
          );
      }
    )
    .join('');
}


/**
 * ============================================================
 * NORMALIZA BOOLEANOS
 * ============================================================
 */
function normalizeBoolean_(
  value,
  defaultValue
) {
  if (
    value === true ||
    value === false
  ) {
    return value;
  }


  const normalized =
    String(
      value === undefined ||
      value === null
        ? ''
        : value
    )
      .trim()
      .toUpperCase();


  if (
    [
      'TRUE',
      '1',
      'SIM',
      'YES',
      'S',
      'Y'
    ].includes(
      normalized
    )
  ) {
    return true;
  }


  if (
    [
      'FALSE',
      '0',
      'NÃO',
      'NAO',
      'NO',
      'N'
    ].includes(
      normalized
    )
  ) {
    return false;
  }


  return defaultValue;
}


/**
 * ============================================================
 * RESPOSTA JSON
 * ============================================================
 */
function jsonOutput_(
  data
) {
  return ContentService
    .createTextOutput(
      JSON.stringify(data)
    )
    .setMimeType(
      ContentService.MimeType.JSON
    );
}
