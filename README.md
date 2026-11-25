# AutoRadio Connect

Central experimental de identificação e diagnóstico de rádios automotivos.

## Alpha 0.1

Interface web responsiva com:
- Seleção de dispositivos via **WebUSB**, quando suportada pelo dispositivo e navegador.
- Descoberta **Bluetooth Low Energy** (BLE); **não** funciona com Bluetooth clássico (A2DP/HFP) de muitos rádios.
- Coleta local de descritores USB disponíveis (fabricante, produto, VID/PID, interfaces), sem abrir interfaces nem enviar comandos.
- Pesquisa manual de possíveis atualizações no site da Pioneer.
- Relatório JSON exportável.

**Importante:** conectar o cabo USB não garante que um rádio seja detectável ou controlável. Vários modelos usam a porta apenas para mídia/pendrive. Nenhuma função de reset, atualização de firmware, busca de estações ou pareamento automático está habilitada. Não existe instalação universal de drivers de rádio.

## Como abrir
Acesse `index.html` em uma hospedagem **HTTPS**, como GitHub Pages. Para publicar: abra **Settings → Pages**, escolha **Deploy from a branch**, selecione `main` e `/(root)` e salve. A publicação poderá levar alguns minutos.

URL esperada, após ativação: https://luissimoes0106-lab.github.io/autoradio-connect/

Preferencialmente use **Chrome no Android** com cabo USB de dados e adaptador OTG, quando necessário. Navegadores podem bloquear determinados dispositivos e portas. O identificador USB pode pertencer ao chipset ou à interface USB do aparelho, não ao modelo comercial.

## Primeiro teste seguro

1. Com o carro estacionado, ligue o rádio normalmente. Não experimente flash/reset durante condução.
2. Abra a aplicação pelo endereço HTTPS no Chrome do Android.
3. Toque em **Conectar por USB** e observe se o rádio aparece no seletor. Não force permissões.
4. Selecione apenas o rádio reconhecido e exporte o relatório. Não conecte dispositivos desconhecidos.
5. Anote o **modelo exato** (etiqueta frontal/lateral ou menu de informações) e a versão atual de firmware, se disponível.
6. Pesquise o firmware usando o modelo exato e procedimento oficial da região antes de qualquer atualização futura.

## Próximas etapas
- Catalogar modelos Pioneer e procedimentos oficiais de identificação, atualização e reset.
- Adicionar guia assistido específico do modelo e relatório legível.
- Testar suporte nativo Android USB Host quando as APIs web não forem suficientes.
- Implementar comunicação ativa **somente** para protocolos documentados e aparelhos comprovadamente compatíveis, com consentimento explícito e etapas de recuperação.

Não use este protótipo como ferramenta de reparação: é apenas descoberta, não diagnóstico eletrônico profundo.
