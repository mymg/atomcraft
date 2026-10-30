# ⚛️ AtomCraft

Mod para **Minecraft 1.21.1 (NeoForge)** em que todo item tem uma **composição química**.
Você pode **atomizar** itens, guardando seus elementos num estoque, e depois usar uma máquina
para **reconstruir qualquer item** — desde que tenha os elementos necessários guardados.

> Exemplo: um **balde de água** é formado por H₂O (Hidrogênio + Oxigênio) mais o Ferro do balde.
> Atomize-o e o estoque recebe `H:2 O:1 Fe:3`. Com esses elementos, o Reconstrutor cria outro balde de água.

## Como funciona

| Bloco | Uso |
|---|---|
| **Atomizador** | Clique com um item na mão → o item é decomposto e os elementos vão para o estoque. Agachado (Shift) atomiza a pilha toda. |
| **Reconstrutor Molecular** | Clique com um item na mão (ele serve de **molde** e não é gasto) → cria uma cópia consumindo os elementos. Shift cria até uma pilha. Mão vazia mostra o estoque. |

Comandos:

- `/atomcraft estoque` — lista os elementos guardados
- `/atomcraft composicao` — mostra a fórmula do item na mão

Receitas: o **Atomizador** usa ferro, vidro, redstone e um alto-forno; o **Reconstrutor** usa ferro, diamante, redstone e uma bancada de trabalho.

## Composições químicas (data-driven)

As composições ficam em arquivos JSON, então qualquer **data pack** ou outro mod pode adicionar ou alterar itens sem mexer no código:

```
src/main/resources/data/atomcraft/compositions/sugar.json
```

```json
{
  "item": "minecraft:sugar",
  "elements": { "C": 12, "H": 22, "O": 11 }
}
```

Os elementos são referenciados pelo símbolo químico. Os disponíveis estão em
[`ChemicalElement.java`](src/main/java/com/atomcraft/chemistry/ChemicalElement.java) — para adicionar um novo, basta incluir uma linha no enum (e a tradução em `lang/`).

Itens já mapeados: balde, balde de água, gelo, lingotes de ferro/ouro/cobre, carvão, diamante, areia, vidro, açúcar, tronco de carvalho, farinha de osso, pólvora e pedra.

## Estrutura do projeto

```
src/main/java/com/atomcraft/
├── AtomCraft.java                 # classe principal do mod
├── chemistry/
│   ├── ChemicalElement.java       # tabela de elementos
│   ├── Composition.java           # composição de um item (elemento → quantidade)
│   └── CompositionManager.java    # carrega data/*/compositions/*.json
├── storage/
│   └── ElementStorage.java        # "sistema" onde os elementos ficam guardados (salvo no mundo)
├── block/
│   ├── AtomizerBlock.java         # máquina que atomiza
│   ├── AssemblerBlock.java        # máquina que reconstrói
│   └── ElementStorageReport.java
├── registry/ModBlocks.java        # registro de blocos e itens
└── command/AtomCraftCommand.java  # /atomcraft
```

## Rodando em desenvolvimento

Pré-requisito: **JDK 21**.

```bash
./gradlew runClient   # abre o Minecraft com o mod
./gradlew build       # gera o .jar em build/libs/
```

No IntelliJ IDEA basta abrir a pasta como projeto Gradle.

## Roadmap

- [ ] Interface gráfica (GUI) nas máquinas, com inventário e barra de progresso
- [ ] Consumo de energia (Forge Energy) e tempo de processamento
- [ ] Estoque por jogador/time em vez de um estoque global do mundo
- [ ] Tooltip mostrando a fórmula química de cada item
- [ ] Composição automática a partir das receitas de crafting (soma dos ingredientes)
- [ ] Integração com JEI/EMI
- [ ] Item "Tanque de Elemento" para transportar elementos fisicamente
- [ ] Mais elementos e composições para todos os itens vanilla

## Licença

[MIT](LICENSE)
