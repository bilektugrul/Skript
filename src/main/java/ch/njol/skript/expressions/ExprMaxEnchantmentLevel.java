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

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

public class ExprMaxEnchantmentLevel extends SimpleExpression<Integer> {
	
	static {
		Skript.registerExpression(ExprMaxEnchantmentLevel.class, Integer.class, ExpressionType.PROPERTY, "max[imum] [ench[antment]] (level|lvl) of %enchantment%");
	}
	
	@SuppressWarnings("null")
	Expression<Enchantment> ench;
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		ench = (Expression<Enchantment>) exprs[0];
		return true;
	}
	
	@Nullable
	@Override
	@SuppressWarnings("null")
	protected Integer[] get(Event e) {
		return new Integer[]{ench.getSingle(e).getMaxLevel()};
	}
	
	@Override
	public boolean isSingle() {
		return true;
	}
	
	@Override
	public Class<? extends Integer> getReturnType() {
		return Integer.class;
	}
	
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "the maximum enchantment level of " + ench.toString(e, debug);
	}
	
}
