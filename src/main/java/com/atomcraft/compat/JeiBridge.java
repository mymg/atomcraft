package com.atomcraft.compat;

/**
 * Ponte com o JEI sem depender dele: o plugin do JEI (se instalado) preenche estes ganchos.
 * Os dados das receitas chegam do servidor depois de o JEI montar as listas, então avisamos para atualizar.
 */
public final class JeiBridge {

    /** Chamado quando valores atômicos ou receitas da Forja chegam do servidor. */
    public static Runnable onDataChanged = () -> {};
    /** Abre a lista de receitas de uma categoria do mod (vitrine). Recebe o id da categoria. */
    public static java.util.function.Consumer<String> showCategory = id -> {};

    private JeiBridge() {}

    public static void dataChanged() {
        try {
            onDataChanged.run();
        } catch (RuntimeException e) {
            com.atomcraft.AtomCraft.LOGGER.warn("AtomCraft: falha ao atualizar o JEI", e);
        }
    }
}
