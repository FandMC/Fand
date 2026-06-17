package io.fand.server.protocol;

import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.commands.ViaCommandHandler;
import com.viaversion.viaversion.platform.UserConnectionViaVersionPlatform;
import com.viaversion.viaversion.platform.ViaDecodeHandler;
import com.viaversion.viaversion.platform.ViaEncodeHandler;
import com.viaversion.viaversion.platform.ViaChannelInitializer;
import com.viaversion.viaversion.protocol.ServerProtocolVersionSingleton;
import com.viaversion.viaversion.libs.gson.JsonObject;
import io.netty.channel.ChannelPipeline;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import net.minecraft.SharedConstants;
import net.minecraft.network.HandlerNames;
import org.slf4j.LoggerFactory;

public final class FandViaVersion {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FandViaVersion.class);
    private final Platform platform;
    private final Injector injector = new Injector();
    private final AtomicBoolean enabled = new AtomicBoolean();
    private final AtomicBoolean initialized = new AtomicBoolean();

    public FandViaVersion(Path root) {
        this.platform = new Platform(root.resolve("viaversion").toFile());
    }

    public void configure(boolean enabled) {
        this.enabled.set(enabled);
        if (enabled) {
            init();
        }
    }

    public void inject(ChannelPipeline pipeline) {
        if (!enabled.get()) {
            return;
        }
        init();
        if (pipeline.get(ViaDecodeHandler.NAME) != null || pipeline.get(ViaEncodeHandler.NAME) != null) {
            return;
        }
        UserConnection connection = ViaChannelInitializer.createUserConnection(pipeline.channel(), false);
        pipeline.addAfter(HandlerNames.SPLITTER, ViaDecodeHandler.NAME, new ViaDecodeHandler(connection));
        pipeline.addAfter(HandlerNames.PREPENDER, ViaEncodeHandler.NAME, new ViaEncodeHandler(connection));
    }

    private void init() {
        if (Via.isLoaded()) {
            initialized.set(true);
            return;
        }
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        try {
            var manager = ViaManagerImpl.builder()
                    .platform(platform)
                    .injector(injector)
                    .loader(ViaPlatformLoader.NOOP)
                    .commandHandler(new ViaCommandHandler())
                    .build();
            Via.init(manager);
            manager.init();
            manager.onServerLoaded();
            LOGGER.info("Embedded ViaVersion enabled for Fand protocol compatibility");
        } catch (RuntimeException ex) {
            initialized.set(false);
            enabled.set(false);
            throw ex;
        }
    }

    private static final class Injector implements ViaInjector {

        @Override
        public void inject() {
        }

        @Override
        public void uninject() {
        }

        @Override
        public ProtocolVersion getServerProtocolVersion() {
            return serverProtocol();
        }

        @Override
        public java.util.SortedSet<ProtocolVersion> getServerProtocolVersions() {
            return new java.util.TreeSet<>(java.util.Set.of(serverProtocol()));
        }

        @Override
        public JsonObject getDump() {
            var dump = new JsonObject();
            dump.addProperty("type", "fand");
            dump.addProperty("serverProtocol", serverProtocol().getName());
            return dump;
        }

        private static ProtocolVersion serverProtocol() {
            var protocol = SharedConstants.getCurrentVersion().protocolVersion();
            var known = ProtocolVersion.getProtocol(protocol);
            return known.isKnown() ? known : ProtocolVersion.v26_1;
        }
    }

    private static final class Platform extends UserConnectionViaVersionPlatform {

        private final Logger logger = new JulBridgeLogger();

        private Platform(File dataFolder) {
            super(dataFolder);
        }

        @Override
        public Logger createLogger(String name) {
            return logger;
        }

        @Override
        public Logger getLogger() {
            return logger;
        }

        @Override
        public String getPlatformName() {
            return "Fand";
        }

        @Override
        public String getPlatformVersion() {
            return "0.1.0-SNAPSHOT";
        }

        @Override
        public String getPluginVersion() {
            return "5.9.1";
        }

        @Override
        public JsonObject getDump() {
            var dump = new JsonObject();
            dump.addProperty("platform", "Fand");
            dump.addProperty("serverProtocol", new ServerProtocolVersionSingleton(Injector.serverProtocol()).protocolVersion().getName());
            return dump;
        }
    }

    private static final class JulBridgeLogger extends Logger {

        private final org.slf4j.Logger delegate = LoggerFactory.getLogger("ViaVersion");

        private JulBridgeLogger() {
            super("ViaVersion", null);
        }

        @Override
        public void log(LogRecord record) {
            Objects.requireNonNull(record, "record");
            var message = record.getMessage();
            var thrown = record.getThrown();
            if (record.getLevel().intValue() >= Level.SEVERE.intValue()) {
                delegate.error(message, thrown);
            } else if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                delegate.warn(message, thrown);
            } else if (record.getLevel().intValue() >= Level.INFO.intValue()) {
                delegate.info(message, thrown);
            } else {
                delegate.debug(message, thrown);
            }
        }
    }
}
