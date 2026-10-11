package dev.qynl.racers.track;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.net.ModNetworking;

/**
 * "Build a circuit by driving it": records where a car goes, then bakes the
 * route into a real track structure.
 *
 * <p>Everything the Python generator produces can also be produced from a lap
 * driven in game, because both write the same structure layout.
 */
public final class TrackRecorder {
    private static final double SAMPLE_SPACING = 1.0;
    private static final int MAX_SAMPLES = 3000;

    private final MinecraftServer server;
    private final ServerPlayerEntity player;
    private final List<double[]> path = new ArrayList<>();
    private final List<Float> headings = new ArrayList<>();
    private int width = 7;
    private boolean barriers = true;
    private boolean finished;

    public TrackRecorder(MinecraftServer server, ServerPlayerEntity player) {
        this.server = server;
        this.player = player;
        Vec3d pos = player.getPos();
        path.add(new double[]{pos.x, pos.y, pos.z});
        headings.add(player.getYaw());
    }

    public void setWidth(int width) {
        this.width = Math.max(3, Math.min(15, width));
    }

    public void setBarriers(boolean barriers) {
        this.barriers = barriers;
    }

    public int samples() {
        return path.size();
    }

    public boolean isFinished() {
        return finished;
    }

    public double length() {
        double total = 0.0;
        for (int i = 1; i < path.size(); i++) {
            total += Math.hypot(path.get(i)[0] - path.get(i - 1)[0],
                    path.get(i)[2] - path.get(i - 1)[2]);
        }
        return total;
    }

    /** Called every server tick while the recording is running. */
    public void tick() {
        CarEntity car = player.getVehicle() instanceof CarEntity c ? c : null;
        if (car == null) {
            return;
        }
        Vec3d pos = car.getPos();
        double[] last = path.get(path.size() - 1);
        double distance = Math.hypot(pos.x - last[0], pos.z - last[2]);
        if (distance < SAMPLE_SPACING) {
            return;
        }
        if (path.size() >= MAX_SAMPLES) {
            stop();
            return;
        }
        path.add(new double[]{pos.x, pos.y, pos.z});
        headings.add(car.getYaw());
        if (path.size() % 25 == 0) {
            ModNetworking.sendRecordState(player, 1,
                    String.format("%d samples, %.0f blocks", path.size(), length()));
        }
    }

    /** Bakes the recording into a track and registers it. */
    public Track stop() {
        finished = true;
        if (path.size() < 40) {
            player.sendMessage(Text.translatable("message.fabricracers.record_too_short")
                    .formatted(Formatting.RED), false);
            TrackManager.get(server).stopRecording(player);
            return null;
        }
        // drop the tail: the start and the end would otherwise overlap
        int count = path.size();
        if (Math.hypot(path.get(0)[0] - path.get(count - 1)[0],
                path.get(0)[2] - path.get(count - 1)[2]) < 12.0) {
            path.remove(count - 1);
            headings.remove(headings.size() - 1);
            count--;
        }

        double minX = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        for (double[] point : path) {
            minX = Math.min(minX, point[0]);
            minZ = Math.min(minZ, point[2]);
            minY = Math.min(minY, point[1]);
        }
        int pad = width + 6;
        int sizeX = (int) Math.ceil((maxX() - minX)) + pad * 2 + 2;
        int sizeZ = (int) Math.ceil((maxZ() - minZ)) + pad * 2 + 2;
        int sizeY = 10;

        TrackStructure.Builder builder = new TrackStructure.Builder(sizeX, sizeY, sizeZ);
        // recorded circuits are flattened to the level the lap started on, which
        // keeps grid slots, checkpoints and the road on the same plane
        double baseY = path.get(0)[1];
        int half = (width - 1) / 2;

        // road, kerbs, verge
        for (int i = 0; i < path.size(); i++) {
            double[] point = path.get(i);
            double[] next = path.get((i + 1) % path.size());
            double dx = next[0] - point[0];
            double dz = next[2] - point[2];
            double length = Math.max(1.0e-4, Math.hypot(dx, dz));
            double rx = -dz / length;
            double rz = dx / length;
            int px = (int) Math.round(point[0] - minX) + pad;
            int pz = (int) Math.round(point[2] - minZ) + pad;
            final int py = 1;
            builder.set(px, 0, pz, "minecraft:dirt");
            for (int o = -half; o <= half; o++) {
                int x = (int) Math.round(point[0] - minX) + pad + (int) Math.round(rx * o);
                int z = (int) Math.round(point[2] - minZ) + pad + (int) Math.round(rz * o);
                builder.set(x, 0, z, "minecraft:dirt");
                builder.set(x, py, z, "fabricracers:asphalt");
            }
            for (int side = -1; side <= 1; side += 2) {
                int x = (int) Math.round(point[0] - minX) + pad
                        + (int) Math.round(rx * (half + 1) * side);
                int z = (int) Math.round(point[2] - minZ) + pad
                        + (int) Math.round(rz * (half + 1) * side);
                String color = ((i / 2) % 2 == 0) ? "red" : "white";
                builder.set(x, py, z, "fabricracers:kerb", "color", color);
                if (barriers) {
                    int bx = (int) Math.round(point[0] - minX) + pad
                            + (int) Math.round(rx * (half + 2) * side);
                    int bz = (int) Math.round(point[2] - minZ) + pad
                            + (int) Math.round(rz * (half + 2) * side);
                    builder.set(bx, py + 1, bz, "fabricracers:safety_barrier");
                    builder.set(bx, py + 2, bz, "fabricracers:safety_barrier");
                }
            }
        }

        // checkpoints + start line every lap-ish
        int checkpointSpacing = Math.max(20, path.size() / 12);
        for (int i = 0; i < path.size(); i += checkpointSpacing) {
            double[] point = path.get(i);
            double[] next = path.get((i + 8) % path.size());
            double dx = next[0] - point[0];
            double dz = next[2] - point[2];
            double length = Math.max(1.0e-4, Math.hypot(dx, dz));
            double rx = -dz / length;
            double rz = dx / length;
            for (int o = -half; o <= half; o++) {
                int x = (int) Math.round(point[0] - minX) + pad + (int) Math.round(rx * o);
                int z = (int) Math.round(point[2] - minZ) + pad + (int) Math.round(rz * o);
                builder.set(x, 2, z, "fabricracers:checkpoint_marker", "lit", "false");
            }
        }

        TrackStructure structure = builder.build();
        String id = "player_" + player.getName().getString().toLowerCase().replaceAll("[^a-z0-9]", "");
        if (TrackManager.get(server).track(id) != null) {
            id = id + "_" + (int) (System.currentTimeMillis() % 10000);
        }
        ModNetworking.sendRecordState(player, 2, id);
        return finishRegistration(id, structure, baseY, pad, minX, minZ);
    }

    private Track finishRegistration(String id, TrackStructure structure, double baseY, int pad,
                                     double minX, double minZ) {
        TrackManager manager = TrackManager.get(server);
        String json = "{\"name\":\"" + player.getName().getString() + "'s circuit\","
                + "\"short_name\":\"REC\",\"structure\":\"fabricracers:tracks/" + id + "\","
                + "\"width\":" + width + ",\"default_laps\":2,\"author\":\""
                + player.getName().getString() + "\""
                + ",\"grid\":" + gridJson(pad, minX, minZ)
                + ",\"checkpoints\":" + checkpointJson(pad, minX, minZ)
                + "}";
        Track track;
        try {
            track = Track.fromJson(id, com.google.gson.JsonParser.parseString(json).getAsJsonObject(),
                    structure.size(), false);
        } catch (Exception exception) {
            player.sendMessage(Text.translatable("message.fabricracers.record_failed")
                    .formatted(Formatting.RED), false);
            TrackManager.get(server).stopRecording(player);
            return null;
        }
        manager.registerTrack(id, track, structure);
        BlockPos anchor = new BlockPos((int) Math.floor(minX) - pad, (int) Math.floor(baseY),
                (int) Math.floor(minZ) - pad);
        manager.placeTrack(player, track, anchor, 0);
        player.sendMessage(Text.translatable("message.fabricracers.record_done", track.name,
                structure.size()[0] + "x" + structure.size()[2]).formatted(Formatting.GREEN), false);
        dev.qynl.racers.stats.StatsManager.get(server).get(player).tracksRecorded++;
        dev.qynl.racers.stats.StatsManager.get(server).markDirty();
        manager.stopRecording(player);
        return track;
    }

    private double maxX() {
        double max = -Double.MAX_VALUE;
        for (double[] point : path) {
            max = Math.max(max, point[0]);
        }
        return max;
    }

    private double maxZ() {
        double max = -Double.MAX_VALUE;
        for (double[] point : path) {
            max = Math.max(max, point[2]);
        }
        return max;
    }

    private String gridJson(int pad, double minX, double minZ) {
        StringBuilder builder = new StringBuilder("[");
        int rows = Math.min(8, Math.max(4, path.size() / 60));
        for (int i = 0; i < rows; i++) {
            int index = (path.size() - 1 - i * 3 + path.size()) % path.size();
            double[] point = path.get(index);
            double[] behind = path.get((index + 2) % path.size());
            double dx = point[0] - behind[0];
            double dz = point[2] - behind[2];
            double length = Math.max(1.0e-4, Math.hypot(dx, dz));
            double rx = -dz / length;
            double rz = dx / length;
            double lane = (i % 2 == 0 ? 1.4 : -1.4);
            double x = point[0] + rx * lane - minX + pad;
            double z = point[2] + rz * lane - minZ + pad;
            if (i > 0) {
                builder.append(',');
            }
            builder.append('[').append(round(x)).append(",2,").append(round(z)).append(']');
        }
        return builder.append(']').toString();
    }

    private String checkpointJson(int pad, double minX, double minZ) {
        StringBuilder builder = new StringBuilder("[");
        int spacing = Math.max(20, path.size() / 10);
        boolean first = true;
        for (int i = 0; i < path.size(); i += spacing) {
            double[] point = path.get(i);
            double[] next = path.get((i + 6) % path.size());
            double dx = next[0] - point[0];
            double dz = next[2] - point[2];
            double yaw = Math.toDegrees(Math.atan2(-dx, dz));
            double x = point[0] - minX + pad;
            double z = point[2] - minZ + pad;
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('[').append(round(x)).append(",2,").append(round(z)).append(',')
                    .append(round(yaw)).append(']');
        }
        return builder.append(']').toString();
    }

    private static String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
