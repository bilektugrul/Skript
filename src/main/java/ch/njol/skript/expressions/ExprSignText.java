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

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.Event;
import org.bukkit.event.block.SignChangeEvent;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

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
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;

/**
 * @author Peter Güttinger
 */
@Name("Sign Text")
@Description("A line of text on a sign. Can be changed, but remember that there is a 16 character limit per line (including colour codes that use 2 characters each).")
@Examples({"on rightclick on sign:",
		"	line 2 of the clicked block is \"[Heal]\":",
		"		heal the player",
		"	set line 3 to \"%player%\""})
@Since("1.3")
public class ExprSignText extends SimpleExpression<String> {

	static {
		Skript.registerExpression(ExprSignText.class, String.class, ExpressionType.PROPERTY,
				"[the] line %number% [of %block%]",
			"[the] (1¦1st|1¦first|2¦2nd|2¦second|3¦3rd|3¦third|4¦4th|4¦fourth) line [of %block%]",
			"[the] [all] lines [of %block%]"
		);
	}
	
	private static final ItemType sign = Aliases.javaItemType("sign");
	
	@SuppressWarnings("null")
	private Expression<Number> line;
	@SuppressWarnings("null")
	private Expression<Block> block;
	private boolean allLines = false;

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		if (matchedPattern == 0) {
			line = (Expression<Number>) exprs[0];
		} else {
			line = new SimpleLiteral<>(parseResult.mark, false);
		}
		allLines = matchedPattern == 2;
		block = (Expression<Block>) exprs[exprs.length - 1];
		return true;
	}
	
	@Override
	@Nullable
	protected String[] get(Event e) {
		final Number l = line.getSingle(e);
		if (l == null) {
			return new String[0];
		}
		final int line = l.intValue() - 1;
		if ((line < 0 || line > 3) && !allLines) {
			return new String[0];
		}

		if (getTime() >= 0 && block.isDefault() && e instanceof SignChangeEvent && !Delay.isDelayed(e)) {
			SignChangeEvent event = (SignChangeEvent) e;
			if (allLines) {
				return event.getLines();
			}
			return new String[] {event.getLine(line)};
		}
		final Block b = block.getSingle(e);
		if (b == null) {
			return new String[0];
		} if (!sign.isOfType(b)) {
			return new String[0];
		}
		Sign signBlock = (Sign) b.getState();
		return allLines ? signBlock.getLines() : new String[]{signBlock.getLine(line)};
	}
	
	// TODO allow add, remove, and remove all (see ExprLore)
	@Override
	@Nullable
	public Class<?>[] acceptChange(final ChangeMode mode) {
		if (mode == ChangeMode.DELETE || mode == ChangeMode.SET) {
			return new Class[]{String.class};
		}
		return null;
	}
	
	static boolean hasUpdateBooleanBoolean = true;
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void change(final Event e, final @Nullable Object[] delta, final ChangeMode mode) throws UnsupportedOperationException {
		final Number l = line.getSingle(e);
		if (l == null) {
			return;
		}
		final int line = l.intValue() - 1;
		if ((line < 0 || line > 3) && !allLines) {
			return;
		}
		final Block b = block.getSingle(e);
		if (b == null) {
			return;
		}
		if (getTime() >= 0 && e instanceof SignChangeEvent && b.equals(((SignChangeEvent) e).getBlock()) && !Delay.isDelayed(e)) {
			SignChangeEvent event = (SignChangeEvent) e;
			switch (mode) {
				case DELETE:
					if (allLines) {
						setAllLines(null, "", event);
						break;
					}
					event.setLine(line, "");
					break;
				case SET:
					assert delta != null;
					String lineString = (String) delta[0];
					if (allLines) {
						setAllLines(null, lineString, event);
						break;
					}
					event.setLine(line, lineString);
					break;
			}
		} else {
			if (!sign.isOfType(b)) {
				return;
			}
			final Sign s = (Sign) b.getState();
			switch (mode) {
				case DELETE:
					if (allLines) {
						setAllLines(s, "", null);
						break;
					}
					s.setLine(line, "");
					break;
				case SET:
					assert delta != null;
					String lineString = (String) delta[0];
					if (allLines) {
						setAllLines(s, lineString, null);
						break;
					}
					s.setLine(line, lineString);
					break;
			}
			if (hasUpdateBooleanBoolean) {
				try {
					s.update(false, false);
				} catch (final NoSuchMethodError err) {
					hasUpdateBooleanBoolean = false;
					s.update();
				}
			} else {
				s.update();
			}
		}
	}

	private void setAllLines(@Nullable Sign sign, @NonNull String line, @Nullable SignChangeEvent event) {
		for (int i = 0; i < 4; i++) {
			if (event != null) {
				event.setLine(i, line);
			} else {
				sign.setLine(i, line);
			}
		}
	}
	
	@Override
	public boolean setTime(final int time) {
		return super.setTime(time, SignChangeEvent.class, block);
	}

	@Override
	public boolean isSingle() {
		return allLines;
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return allLines ? "all lines of " + block.toString(e, debug) : "line " + line.toString(e, debug) + " of " + block.toString(e, debug);
	}
	
}
