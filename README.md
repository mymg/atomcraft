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
| **Reator de Fusão** | blocos de ferro, cristais do End, blocos de diamante, estrela do Nether |
| **Discos** 16K / 64K / 256K / 1M átomos | ametista e redstone + ferro / ouro / diamante / casco de shulker |

Os discos guardam os elementos no próprio item, então você pode tirá-los do Drive e levá-los para outra base.

## Energia

O mod gera a própria energia (FE), sem depender de outro mod. Os geradores mandam energia para as máquinas
encostadas, para qualquer cabo FE de outros mods e para todas as máquinas da rede pelo **Cabo Atômico**.

| Gerador | Combustível | Geração (padrão) |
|---|---|---|
| **Combustão** (inicial) | qualquer item com carbono. Rende `C + H/4 − O` unidades × 1.600 FE: carvão 38.400 FE, tronco 22.400 FE | 60 FE/t |
| **Carbono** | carbono (C) guardado na rede, 2.000 FE por átomo | 200 FE/t |
| **Fusão** | 4 H → 1 He a cada 10 ticks. Começa frio e aquece até 100% em ~50 s; o hélio volta para a rede | até 2.000 FE/t |

O **Terminal de Elementos** mostra todo o estoque da rede organizado como uma tabela periódica.

## Comandos

- `/atomcraft valor`: mostra fórmula, raridade, energia, tempo e pesquisa do item na mão
- `/atomcraft pesquisa desbloquear`: pesquisa o item na mão (operadores)
- `/atomcraft pesquisa tudo`: pesquisa todos os itens (operadores, útil para testes)

## Configuração

`config/atomcraft-common.toml`: FE por massa atômica (Replicador e Atomizador; 0 desliga o consumo do Atomizador),
multiplicadores de tempo, capacidade e entrada de energia das máquinas, ticks por item no Analisador e
geração, capacidade e saída de cada gerador.

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
- [ ] Suporte a fluidos (tanques e fluid pipes)
- [ ] Texturas e modelos definitivos
- [ ] Integração com JEI/EMI (mostrar fórmula e custo)
- [ ] Terminal para acessar a rede à distância
- [ ] Upgrades de velocidade e eficiência

## Licença

[MIT](LICENSE)
