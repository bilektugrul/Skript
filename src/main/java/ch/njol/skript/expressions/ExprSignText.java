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

import java.util.ArrayList;
import java.util.List;

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
				"[the] line %number% [of %blocks%]",
			"[the] (1¦1st|1¦first|2¦2nd|2¦second|3¦3rd|3¦third|4¦4th|4¦fourth) line [of %blocks%]"
		);
	}
	
	private static final ItemType sign = Aliases.javaItemType("sign");
	
	@SuppressWarnings("null")
	private Expression<Number> line;
	@SuppressWarnings("null")
	private Expression<Block> blocks;
	private boolean multiple = false;

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (matchedPattern == 0) {
			line = (Expression<Number>) exprs[0];
		} else {
			line = new SimpleLiteral<>(parseResult.mark, false);
		}
		blocks = (Expression<Block>) exprs[exprs.length - 1];
		multiple = !blocks.isSingle();
		return true;
	}
	
	@Override
	@Nullable
	protected String[] get(Event e) {
		Number l = line.getSingle(e);

		if (l == null)
			return new String[0];

		int line = l.intValue() - 1;
		if (line < 0 || line > 3)
			return new String[0];

		Block[] blocks = this.blocks.getArray(e);
		if (blocks.length == 0)
			return new String[0];

		List<String> lines = new ArrayList<>();

		boolean isSignChangeEvent = e instanceof SignChangeEvent;
		if (isSignChangeEvent) {
			SignChangeEvent event = (SignChangeEvent) e;
			if (getTime() == -1) {
				lines.add(((Sign) event.getBlock().getState()).getLine(line));
			} else if (getTime() >= 0 && this.blocks.isDefault() && !Delay.isDelayed(e)) {
				lines.add(event.getLine(line));
			}
		}

		if (!multiple && (isSignChangeEvent && blocks[0].equals(((SignChangeEvent) e).getBlock())))
			return lines.toArray(String[]::new);

		for (Block block : blocks) {
			if (!sign.isOfType(block))
				continue;

			Sign sign = (Sign) block.getState();
			lines.add(sign.getLine(line));
		}

		return lines.toArray(String[]::new);
	}
	
	// TODO allow add, remove, and remove all (see ExprLore)
	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (mode == ChangeMode.DELETE || mode == ChangeMode.SET)
			return new Class[]{String.class};
		return null;
	}
	
	private static boolean hasUpdateBooleanBoolean = true;
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) throws UnsupportedOperationException {
		Number l = line.getSingle(e);
		if (l == null)
			return;

		int line = l.intValue() - 1;
		if (line < 0 || line > 3)
			return;

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
							event.setLine(line, "");
							break;
						case SET:
							assert delta != null;
							event.setLine(line, (String) delta[0]);
							break;
					}
				}
			}

			Sign s = (Sign) block.getState();
			switch (mode) {
				case DELETE:
					s.setLine(line, "");
					break;
				case SET:
					assert delta != null;
					s.setLine(line, (String) delta[0]);
					break;
			}
			if (hasUpdateBooleanBoolean) {
				try {
					s.update(false, false);
				} catch (NoSuchMethodError err) {
					hasUpdateBooleanBoolean = false;
					s.update();
				}
			} else {
				s.update();
			}
		}
	}

	@Override
	public boolean setTime(int time) {
		return super.setTime(time, SignChangeEvent.class, blocks);
	}

	@Override
	public boolean isSingle() {
		return blocks.isSingle();
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "line " + line.toString(e, debug) + " of " + blocks.toString(e, debug);
	}
	
}
