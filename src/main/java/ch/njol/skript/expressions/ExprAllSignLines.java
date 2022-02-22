/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.Aliases;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.Event;
import org.bukkit.event.block.SignChangeEvent;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Name("All Sign Lines")
@Description("All lines of a sign. Can be changed, but remember that there is a 16 character limit per line (including colour codes that use 2 characters each).")
@Examples({"on rightclick on sign:",
	"	all lines of clicked sign contains \"[Kill Me]\":",
	"		kill the player",
	"	set line 3 to \"%player%\""})
@Since("INSERT VERSION")
public class ExprAllSignLines extends SimpleExpression<String> {

	static {
		Skript.registerExpression(ExprAllSignLines.class, String.class, ExpressionType.PROPERTY, "[the] [all] lines [of %blocks%]");
	}

	private static final ItemType sign = Aliases.javaItemType("sign");

	@SuppressWarnings("null")
	private Expression<Block> blocks;
	private boolean multiple = false;

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		blocks = (Expression<Block>) exprs[exprs.length - 1];
		multiple = !blocks.isSingle();
		return true;
	}

	@Override
	@Nullable
	protected String[] get(Event e) {
		Block[] blocks = this.blocks.getArray(e);
		if (blocks.length == 0)
			return new String[0];

		List<String> lines = new ArrayList<>();

		boolean isSignChangeEvent = e instanceof SignChangeEvent;
		if (isSignChangeEvent) {
			SignChangeEvent event = (SignChangeEvent) e;
			if (getTime() == -1) {
				lines.addAll(List.of(((Sign) event.getBlock()).getLines()));
			} else if (getTime() >= 0 && this.blocks.isDefault() && !Delay.isDelayed(e)) {
				lines.addAll(List.of(event.getLines()));
			}
		}

		if (!multiple && (isSignChangeEvent && blocks[0].equals(((SignChangeEvent) e).getBlock())))
			return lines.toArray(String[]::new);

		for (Block block : blocks) {
			if (!sign.isOfType(block))
				continue;

			Sign sign = (Sign) block.getState();
			lines.addAll(Arrays.asList(sign.getLines()));
		}

		return lines.toArray(String[]::new);
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (mode == ChangeMode.DELETE || mode == ChangeMode.SET)
			return new Class[]{String.class};
		return null;
	}

	private static boolean hasForceUpdate = true;

	@SuppressWarnings("incomplete-switch")
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) throws UnsupportedOperationException {
		Block[] blocks = this.blocks.getArray(e);
		if (blocks.length == 0)
			return;

		for (Block block : blocks) {
			if (!sign.isOfType(block))
				continue;

			if (e instanceof SignChangeEvent && block.equals(((SignChangeEvent) e).getBlock())) {
				SignChangeEvent event = (SignChangeEvent) e;
				if (getTime() >= 0 && !Delay.isDelayed(e)) {
					switch (mode) {
						case DELETE:
							setAllLines(null, "", event);
							break;
						case SET:
							assert delta != null;
							setAllLines(null, (String) delta[0], event);
							break;
					}
				}
			}

			Sign s = (Sign) block.getState();
			switch (mode) {
				case DELETE:
					setAllLines(s, "", null);
					break;
				case SET:
					assert delta != null;
					setAllLines(s, (String) delta[0], null);
					break;
			}
			if (hasForceUpdate) {
				s.update(false, false);
			} else {
				try {
					s.update(false, false);
					hasForceUpdate = true;
				} catch (NoSuchMethodError err) {
					hasForceUpdate = false;
					s.update();
				}
			}
		}
	}

	private void setAllLines(@Nullable Sign sign, @NonNull String line, @Nullable SignChangeEvent event) {
		for (int i = 0; i < 4; i++) {
			if (event != null)
				event.setLine(i, line);
			else
				sign.setLine(i, line);
		}
	}

	@Override
	public boolean setTime(int time) {
		return super.setTime(time, SignChangeEvent.class, blocks);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "all lines of " + blocks.toString(e, debug);
	}

}
