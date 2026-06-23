package io.fand.api.entity;

/**
 * Snapshot of the movement keys a player is holding.
 *
 * <p>The fields match vanilla's serverbound player-input packet:
 * forward/backward, left/right, jump, sneak, and sprint. Plugins can use this
 * to inspect or drive simulated players without depending on server internals.
 */
public record PlayerInput(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean jump,
        boolean sneak,
        boolean sprint
) {

    public static final PlayerInput NONE = new PlayerInput(false, false, false, false, false, false, false);

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .forward(forward)
                .backward(backward)
                .left(left)
                .right(right)
                .jump(jump)
                .sneak(sneak)
                .sprint(sprint);
    }

    public static final class Builder {
        private boolean forward;
        private boolean backward;
        private boolean left;
        private boolean right;
        private boolean jump;
        private boolean sneak;
        private boolean sprint;

        private Builder() {
        }

        public Builder forward(boolean forward) {
            this.forward = forward;
            return this;
        }

        public Builder backward(boolean backward) {
            this.backward = backward;
            return this;
        }

        public Builder left(boolean left) {
            this.left = left;
            return this;
        }

        public Builder right(boolean right) {
            this.right = right;
            return this;
        }

        public Builder jump(boolean jump) {
            this.jump = jump;
            return this;
        }

        public Builder sneak(boolean sneak) {
            this.sneak = sneak;
            return this;
        }

        public Builder sprint(boolean sprint) {
            this.sprint = sprint;
            return this;
        }

        public PlayerInput build() {
            return new PlayerInput(forward, backward, left, right, jump, sneak, sprint);
        }
    }
}
