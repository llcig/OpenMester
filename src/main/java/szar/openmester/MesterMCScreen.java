package szar.openmester;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Unique;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

public class MesterMCScreen extends Screen {
    private final Screen parent;
    @Unique private TextFieldWidget usernameField = null;
//    @Unique private TextFieldWidget passwordField = null;
    @Unique private ButtonWidget loginButton = null;
    @Unique private boolean initialized = false;

    public MesterMCScreen(Screen parent) {
        super(Text.of("MesterMC Login"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (this.initialized) {
            this.addSelectableChild(usernameField);
//            this.addSelectableChild(passwordField);
            this.addDrawableChild(loginButton);
            return;
        }

        this.initialized = true;

        this.usernameField = new TextFieldWidget(
            this.textRenderer,
            (this.width / 2) - (100 / 2),
            (this.height / 2) - (30 / 2) - 50,
            100,
            30,
            Text.of("Username")
        );
        this.addDrawableChild(usernameField);

//        this.passwordField = new TextFieldWidget(
//            this.textRenderer,
//            (this.width / 2) - (100 / 2),
//            (this.height / 2) - (30 / 2) - 15,
//            100,
//            30,
//            Text.of("Password")
//        );
//        this.addDrawableChild(passwordField);

        this.loginButton = ButtonWidget.builder(
            Text.of("Log in"),
            (button) -> {
                try {
                    assert this.client != null;

                    Session session = new Session(
                        usernameField.getText(),
                        UUID.nameUUIDFromBytes(("OfflinePlayer:" + usernameField.getText()).getBytes()),
                        "tkn",
                        Optional.empty(),
                        Optional.empty(),
                        Session.AccountType.LEGACY
                    );

                    Field sessionField = MinecraftClient.class.getDeclaredField("session");
                    sessionField.setAccessible(true);
                    sessionField.set(client, session);

                    button.setMessage(Text.of("Logged in"));
                } catch (Throwable e) {
                    e.printStackTrace();
                    button.setMessage(Text.of("An error occurred"));
                }
            }
        ).width(100).position((this.width / 2) - (100 / 2), (this.height / 2) - (30 / 2) + 20).build();
        this.addDrawableChild(loginButton);
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(this.parent);
    }
}
