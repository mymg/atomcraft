# ⚛️ AtomCraft

Mod para **Minecraft 26.2 (NeoForge)** em que todo item tem uma **composição química**.
Você **atomiza** itens e guarda os elementos em **discos**, **pesquisa** os itens que quer
copiar e usa o **Replicador Atômico** para criá-los de novo a partir dos elementos, gastando energia (FE).

## Como funciona

1. **Monte a rede.** Coloque um **Drive de Elementos** com pelo menos um **Disco de Elementos** dentro.
   Máquinas, Drives e **Cabos Atômicos** que se encostam formam uma única rede, como no Applied Energistics.
2. **Atomize.** O **Atomizador** tem um buffer de 21 slots. Coloque itens à mão ou por hopper, pipe ou esteira.
   Cada item é decomposto e seus elementos vão para os discos, gastando um pouco de energia FE.
   Itens danificados rendem proporcionalmente à durabilidade.
3. **Pesquise.** O **Analisador** pesquisa itens para quem colocou a máquina, como no modo Journey do Terraria.
   Cada unidade analisada é **consumida**. A quantidade exigida depende da raridade do item, e o Analisador
   só aceita a quantidade que ainda falta (se faltam 23 pedras, entram no máximo 23).
4. **Replique.** No **Replicador Atômico**, escolha um item da sua biblioteca, defina a quantidade
   (botões ±10/100/1000, ou ±16/32/64 segurando Shift) e clique em *Replicar*.
   A máquina consome os elementos da rede e energia FE, de qualquer cabo de energia compatível com NeoForge.
   Os itens prontos vão para um buffer de saída de 9 slots, de onde hoppers e pipes podem retirá-los.

A pesquisa é **por jogador**: cada um tem a própria biblioteca. O armazenamento de elementos é **da rede** (discos no Drive).

### Descoberta dos elementos

No começo todos os elementos são **desconhecidos**: o carvão aparece como `??₂₄` e o Terminal mostra `?` no lugar
dos símbolos. Analisar **uma unidade** de um item no Analisador já revela os elementos que ele contém
(analisar um carvão descobre o Carbono). O hélio, que não existe em nenhum item, é descoberto pelo dono do
Reator de Fusão quando ele começa a produzi-lo. A descoberta é por jogador.

## Abas laterais e upgrades

As máquinas têm abas do lado direito da interface, como no Mekanism:

- **Ligar/desligar**: verde ligada, vermelho desligada. Útil principalmente nos geradores que consomem elementos da rede.
  Desligado, o gerador para de queimar mas continua entregando a energia guardada.
- **Informações**: mostra o dono da máquina (no futuro, permissões).
- **Upgrades**: 2 slots para **chips**, até 8 de cada tipo por máquina.
  - **Velocidade**: o tempo de cada operação cai para 1/(1+n), e o custo de energia sobe 25% por chip.
    Nos geradores, a geração por tick sobe 50% por chip.
  - **Eficiência**: o custo de energia cai 20% por chip (acumulativo).
    Nos geradores, cada chip rende +25% de energia por combustível.

## Sintetizador e schematics

Os chips são uma **tecnologia perdida**: só o **Sintetizador** sabe fabricá-los, e só enquanto a **schematic**
correspondente estiver na biblioteca dele (6 slots). Qualquer jogador pode usar o Sintetizador, e as schematics
podem ser retiradas e emprestadas. Escolha o item clicando nele; a máquina fabrica sem parar enquanto houver
elementos, energia e espaço na saída (use o botão de desligar para parar). O custo é pago em elementos da rede e FE:

| Chip | Elementos | Energia |
|---|---|---|
| Velocidade | Si₃₂ Au₈ Rs₂₄ Cu₈ | 30.000 FE |
| Eficiência | Si₃₂ Au₈ Lm₁₆ Cu₈ | 30.000 FE |

As schematics aparecem em baús de **vilas** (ferreiro, armeiro, cartógrafo, pedreiro, templo), de **fortalezas do Nether**
e **bastiões**, e das **cidades do End**.

## Interfaces

Todas as máquinas usam um tema tecnológico em pixel art, desenhado no próprio código: moldura metálica,
painéis de vidro verde-azulado, barras segmentadas de energia (vermelha), progresso (roxa) e armazenamento (verde).
Os elementos químicos aparecem como badges circulares coloridas com o símbolo; passe o mouse para ver o nome e a quantidade.

## Valor atômico

Cada item tem um valor atômico, que é a composição em átomos mais um nível de raridade.

- **Tamanho e quantidade**: pepita de ferro = Fe×1, barra = Fe×9, bloco = Fe×81.
- **Raridade**: carvão = C×8, mas diamante = C×512 (o equivalente a 64 carvões).

| Raridade | Unidades a pesquisar | Energia | Tempo |
|---|---|---|---|
| Comum | 50 | ×1 | ×1 |
| Incomum | 20 | ×2 | ×1,5 |
| Raro | 5 | ×4 | ×2 |
| Épico | 2 | ×8 | ×3 |
| Lendário | 1 | ×16 | ×4 |

Itens que não empilham (ferramentas, armaduras) precisam de apenas 1 unidade de pesquisa.

**Energia por unidade** = Σ(átomos × massa atômica) × 2 FE × multiplicador da raridade.
Exemplos: barra de ferro ≈ 1.000 FE, diamante ≈ 49.000 FE, estrela do Nether ≈ 819.000 FE.

### Base manual + derivação pelas receitas

Só os itens básicos (minérios, madeira, terra, comida, materiais de mobs) têm valor definido em JSON.
Os demais são **calculados automaticamente pelas receitas** do jogo e de outros mods. O valor de um item
é a soma dos ingredientes dividida pela quantidade produzida, arredondando para baixo. Assim, atomizar nunca rende mais do
que os ingredientes usados. Itens devolvidos na receita (como os baldes do bolo) são descontados, e, havendo
várias receitas, vale a mais barata.

Para adicionar ou ajustar valores, crie arquivos num data pack:

```
data/<namespace>/atomcraft/compositions/<nome>.json
```

```json
{
  "item": "minecraft:diamond",
  "elements": { "C": 512 },
  "rarity": "rare"
}
```

Use `"tag": "minecraft:logs"` no lugar de `"item"` para definir um grupo inteiro. Se `rarity` for omitido,
vale a raridade vanilla do item. Os elementos disponíveis estão em
[`ChemicalElement.java`](src/main/java/com/atomcraft/chemistry/ChemicalElement.java), incluindo elementos
fictícios para materiais mágicos: Redstônio (Rs), Lumínio (Lm), Endério (En), Netherio (Nt), Sculkio (Sk) e Estelário (Sx).

## Compatibilidade com outros mods

Itens de outros mods ganham composição pelas **tags comuns `c:`** do NeoForge, que mods como All The Ores,
Mekanism, Thermal, Create e Applied Energistics usam. Cada material tem a composição de uma barra (9 átomos),
e as outras formas derivam dela:

| Forma (tag) | Composição |
|---|---|
| `c:ingots/*`, `c:raw_materials/*`, `c:dusts/*`, `c:plates/*` | 1 barra |
| `c:nuggets/*` | 1/9 da barra |
| `c:rods/*`, `c:wires/*` | 1/2 barra |
| `c:gears/*` | 4 barras |
| `c:storage_blocks/*`, `c:storage_blocks/raw_*` | 9 barras |
| `c:ores/*` | 1 barra + SiO₂ |

- **Metais:** estanho (Sn), chumbo (Pb), níquel (Ni), zinco (Zn), alumínio (Al), prata (Ag), ósmio (Os),
  platina (Pt), irídio (Ir) e urânio (U).
- **Ligas:** bronze, latão, aço, invar, constantan e electrum.
- **All the Mods:** Allthemodium, Vibranium e Unobtainium viram os elementos exóticos Ad, Vb e Ub.
- **Gemas e pós:** rubi, safira, peridoto, certus quartz, fluix, enxofre, salitre e pós de carvão, diamante e afins.

Itens de outros mods que não estão em nenhuma dessas tags recebem valor pelas próprias receitas (derivação
normal), desde que os ingredientes tenham valor. Um modpack pode adicionar ou corrigir valores com um datapack
em `data/<namespace>/atomcraft/compositions/`, usando `"item"` ou `"tag"`.

## Blocos e itens

| | Receita |
|---|---|
| **Atomizador** | ferro, ametista, redstone |
| **Analisador** | ferro, ametista, redstone |
| **Replicador Atômico** | diamantes, pérolas do Ender, ferro, bloco de redstone |
| **Drive de Elementos** | ferro, baú, redstone, ametista |
| **Cabo Atômico** (×8) | pepitas de ferro, redstone, ametista |
| **Terminal de Elementos** | ferro, vidro, ametista, redstone |
| **Gerador de Combustão** | ferro, fornalha, cobre, redstone |
| **Gerador de Carbono** | ferro, ametista, Gerador de Combustão, alto-forno |
| **Reator de Fusão** (controlador) | blocos de ferro, cristais do End, blocos de diamante, estrela do Nether |
| **Carcaça do Reator** ×8 | ferro, cobre, redstone |
| **Vidro de Contenção** ×4 | carcaça, vidro |
| **Bobina Magnética** ×2 | cobre, redstone, bloco de ferro |
| **Solenoide Central** | bobinas, ouro, olho do Ender |
| **Porta do Reator** ×2 | carcaça, Cabo Atômico, funil |
| **Sintetizador** | ouro, ametista, redstone, mesa de trabalho |
| **Discos** 16K / 64K / 256K / 1M átomos | ametista e redstone + ferro / ouro / diamante / casco de shulker |

Os discos guardam os elementos no próprio item, então você pode tirá-los do Drive e levá-los para outra base.

## Energia

O mod gera a própria energia (FE), sem depender de outro mod. Os geradores mandam energia para as máquinas
encostadas, para qualquer cabo FE de outros mods e para todas as máquinas da rede pelo **Cabo Atômico**.

| Gerador | Combustível | Geração (padrão) |
|---|---|---|
| **Combustão** (inicial) | qualquer item com carbono. Rende `C + H/4 − O` unidades × 1.600 FE: carvão 38.400 FE, tronco 22.400 FE | 60 FE/t |
| **Carbono** | carbono (C) guardado na rede, 2.000 FE por átomo | 200 FE/t |
| **Fusão** (multibloco) | 4 H → 1 He a cada ciclo de 10 ticks. Começa frio e aquece até 100% em 5 min; o hélio volta para a rede | 2.000 FE/t sem módulos, até 25.000 FE/t com módulos III |

### Reator de Fusão (multibloco 7×7×5)

Um tokamak de formato fixo. Camadas de baixo e de cima: 7×7 com os cantos recortados, de **Carcaça do Reator**,
com o **Solenoide Central** no meio. As três camadas do meio:

```
 . C V C V C .      C = Carcaça do Reator (ou Porta do Reator)
 C . . . . . C      V = Vidro de Contenção (só na camada central; pode ser carcaça)
 V . B B B . V      B = Bobina Magnética
 C . B S B . C      S = Solenoide Central
 V . B B B . V      . = vazio (canal de plasma)
 C . . . . . C      K = Controlador (o bloco "Reator de Fusão"), no centro da frente
 . C V K V C .          da camada central, virado para fora
```

- **Portas do Reator** podem ficar no lugar de qualquer carcaça. Elas ligam o reator à rede de elementos (hidrogênio
  entra, hélio sai) e entregam energia a cabos de qualquer mod. Continuam visíveis com o reator formado.
- O botão **Mostrar estrutura** na interface do controlador marca com partículas coloridas onde falta cada peça.
- Formado, os blocos somem e aparece o modelo grande do reator (GeckoLib), com o plasma animado.
- **Módulos** (aba lateral, 4 slots; só o maior nível de cada tipo vale):

| Módulo | I | II | III |
|---|---|---|---|
| Confinamento Magnético (energia por fusão) | +100% | +200% | +400% |
| Injetor de Combustível (duração do ciclo) | 8 ticks | 6 ticks | 4 ticks |
| Reciclador de Hidrogênio (chance de devolver 1 H) | 25% | 50% | 75% |
| Ignição a Plasma (aquecimento; padrão 5 min) | 2 min | 1 min | 30 s |

As schematics dos módulos são fabricadas no Sintetizador e aparecem em baús: nível I em fortalezas do Nether e bastiões,
nível II em bastiões e cidades do End, nível III em cidades do End e **cidades ancestrais**.

### Modelo do reator no Blockbench

O modelo grande usa o **GeckoLib** (dependência obrigatória). No Blockbench, instale o plugin *GeckoLib Animation Utils*
e crie um projeto **GeckoLib Animated Model** (tipo "Block/Item"). Esse formato não tem o limite de 3×3×3 dos modelos
comuns. Arquivos:

- `assets/atomcraft/geckolib/models/block/fusion_reactor_formed.geo.json`
- `assets/atomcraft/geckolib/animations/block/fusion_reactor_formed.animation.json` (animação `plasma`, em loop
  enquanto o reator está aceso)
- `assets/atomcraft/textures/block/fusion_reactor_formed.png` e `fusion_reactor_formed_glowmask.png` (partes que brilham)

O ponto (0, 0, 0) do modelo é o centro da base da estrutura (7×5×7 blocos = 112×80×112 pixels) e a frente, onde fica o
controlador, aponta para o norte (−Z).

O **Terminal de Elementos** mostra todo o estoque da rede organizado como uma tabela periódica.

## Comandos

- `/atomcraft valor`: mostra fórmula, raridade, energia, tempo e pesquisa do item na mão
- `/atomcraft pesquisa desbloquear`: pesquisa o item na mão (operadores)
- `/atomcraft pesquisa tudo`: pesquisa todos os itens (operadores, útil para testes)

## Configuração

`config/atomcraft-common.toml`: FE por massa atômica (Replicador e Atomizador; 0 desliga o consumo do Atomizador),
multiplicadores de tempo, capacidade e entrada de energia das máquinas, ticks por item no Analisador e
geração, capacidade e saída de cada gerador.

## Editando modelos e texturas

A aparência fica em `src/main/resources/assets/atomcraft/`: `textures/` (PNGs 16×16), `models/` (JSON),
`blockstates/` e `items/`. Use o **Blockbench** para modelos (projeto "Java Block/Item") e qualquer editor de
pixel art para texturas. No jogo, **F3 + T** recarrega os recursos.

Os dados do mod (composições, receitas, loot, traduções) são gerados por `tools/gen_resources.py`
(`python3 tools/gen_resources.py .`, precisa do Pillow). O script **nunca sobrescreve** modelos, blockstates,
definições de itens e texturas que já existem: ele só cria os arquivos iniciais de blocos e itens novos.
`--force` recria tudo do zero.

## Desenvolvimento

Pré-requisito: **JDK 25**.

```bash
./gradlew runClient   # abre o Minecraft com o mod
./gradlew build       # gera o .jar em build/libs/
```

Cada push compila o mod no GitHub Actions e:

- liga um servidor dedicado que executa um autoteste do ciclo completo (atomizar → armazenar → pesquisar → replicar);
- abre o cliente numa tela virtual para conferir texturas e modelos;
- roda o **modo vitrine**, que monta as máquinas com dados de exemplo e publica um print de cada interface
  no branch [`ci-screenshots`](../../tree/ci-screenshots).

O `.jar` fica disponível em **Actions → Artifacts**.

## Roadmap

- [x] Interface gráfica nas máquinas (tema tecnológico)
- [x] Energia FE no Replicador
- [x] Rede de armazenamento com discos
- [x] Pesquisa por jogador
- [x] Valores derivados das receitas
- [x] Geradores próprios (combustão, carbono e fusão)
- [x] Terminal de Elementos (tabela periódica)
- [x] Descoberta dos elementos
- [x] Botão de ligar/desligar, aba de dono e upgrades (velocidade e eficiência)
- [x] Sintetizador e schematics no loot do mundo
- [ ] Suporte a fluidos (tanques e fluid pipes)
- [ ] Texturas e modelos definitivos
- [ ] Integração com JEI/EMI (mostrar fórmula e custo)
- [ ] Terminal para acessar a rede à distância
- [ ] Permissões por máquina

## Licença

[MIT](LICENSE)
