package fr.chestesp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/** Menu Build : schema, selection, remplissage / vidage, auto-build. */
public class BuildScreen extends Screen {

    private final Screen parent;

    public BuildScreen(Screen parent) {
        super(Text.literal("Build / Schémas"));
        this.parent = parent;
    }

    private static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    private static BlockPos aimedAdjacent() {
        MinecraftClient mc = mc();
        if (mc.crosshairTarget instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK) {
            return b.getBlockPos().offset(b.getSide());
        }
        return mc.player != null ? mc.player.getBlockPos() : null;
    }

    private ButtonWidget add(int x, int y, int w, String label, ButtonWidget.PressAction a) {
        return this.addDrawableChild(ButtonWidget.builder(Text.literal(label), a).dimensions(x, y, w, 20).build());
    }

    @Override
    protected void init() {
        int cw = 150;
        int lx = this.width / 2 - cw - 5;
        int rx = this.width / 2 + 5;
        int y = this.height / 2 - 70;

        // ---- colonne gauche : schema
        add(lx, y, cw, "Charger un schéma...", b -> mc().setScreen(new SchematicListScreen(this)));
        add(lx, y + 24, cw, "Origine : bloc visé", b -> {
            BlockPos p = aimedAdjacent();
            if (p != null) BuildState.origin = p;
        });
        add(lx, y + 48, cw, "Origine : ma position", b -> {
            if (mc().player != null) BuildState.origin = mc().player.getBlockPos();
        });
        add(lx, y + 72, cw, "Rotation : " + BuildState.rot * 90 + "°", b -> {
            BuildState.setRotation(BuildState.rot + 1);
            b.setMessage(Text.literal("Rotation : " + BuildState.rot * 90 + "°"));
        });
        add(lx, y + 96, cw, ghostLabel(), b -> {
            BuildState.showGhost = !BuildState.showGhost;
            b.setMessage(Text.literal(ghostLabel()));
        });
        add(lx, y + 120, cw, "Construire le schéma", b -> {
            if (BuildBot.start(mc(), BuildBot.Mode.SCHEM)) mc().setScreen(null);
        });

        // ---- colonne droite : selection
        add(rx, y, cw, "Coin 1 : bloc visé", b -> BuildState.setCorner(mc(), 1));
        add(rx, y + 24, cw, "Coin 2 : bloc visé", b -> BuildState.setCorner(mc(), 2));
        add(rx, y + 48, cw, "Remplir (bloc en main)", b -> {
            if (BuildBot.start(mc(), BuildBot.Mode.FILL)) mc().setScreen(null);
        });
        add(rx, y + 72, cw, "Vider la sélection", b -> {
            if (BuildBot.start(mc(), BuildBot.Mode.CLEAR)) mc().setScreen(null);
        });
        add(rx, y + 96, cw, "STOP build", b -> BuildBot.stop("Build arrêté"));
        add(rx, y + 120, cw, "Effacer la sélection", b -> {
            BuildState.pos1 = null;
            BuildState.pos2 = null;
        });

        // ---- deplacer l'origine
        String[] names = {"X-", "X+", "Y-", "Y+", "Z-", "Z+"};
        int[][] d = {{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};
        int nx = this.width / 2 - 130;
        for (int i = 0; i < 6; i++) {
            final int[] dd = d[i];
            add(nx + i * 44, y + 148, 40, names[i], b -> {
                if (BuildState.origin != null) BuildState.origin = BuildState.origin.add(dd[0], dd[1], dd[2]);
            });
        }

        add(this.width / 2 - 75, y + 174, 150, "Retour", b -> mc().setScreen(parent));
    }

    private static String ghostLabel() {
        return "Plan fantôme : " + (BuildState.showGhost ? "§aON" : "§cOFF");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int top = this.height / 2 - 118;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, top, 0xFFFFFF);

        String s1 = BuildState.schem == null ? "Aucun schéma chargé"
                : BuildState.schemName + "  (" + BuildState.schem.sx + "x" + BuildState.schem.sy + "x" + BuildState.schem.sz + ")";
        if (BuildState.origin != null && BuildState.schem != null) {
            s1 += "  @ " + BuildState.origin.getX() + " " + BuildState.origin.getY() + " " + BuildState.origin.getZ();
        }
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(s1), cx, top + 14, 0xAAAAAA);

        int[] sb = BuildState.selBounds();
        String s2 = sb == null ? "Sélection : définis 2 coins"
                : "Sélection : " + (sb[3] - sb[0] + 1) + "x" + (sb[4] - sb[1] + 1) + "x" + (sb[5] - sb[2] + 1);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(s2), cx, top + 26, 0xAAAAAA);

        if (BuildBot.mode != BuildBot.Mode.NONE) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Build en cours : " + BuildBot.mode), cx, top + 38, 0x55FF55);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
