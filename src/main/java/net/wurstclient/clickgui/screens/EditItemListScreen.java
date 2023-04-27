/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.wurstclient.settings.ItemListSetting;
import net.wurstclient.util.ItemUtils;
import net.wurstclient.util.ListWidget;
import net.wurstclient.util.RenderUtils;

public final class EditItemListScreen extends Screen
{
	private final Screen prevScreen;
	private final ItemListSetting itemList;
	
	private ListGui listGui;
	private TextFieldWidget itemNameField;
	private ButtonWidget addButton;
	private ButtonWidget removeButton;
	private ButtonWidget doneButton;
	
	private Item itemToAdd;
	
	public EditItemListScreen(Screen prevScreen, ItemListSetting itemList)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
		this.itemList = itemList;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(client, this, itemList.getItemNames());
		
		itemNameField = new TextFieldWidget(client.textRenderer,
			width / 2 - 152, height - 55, 150, 18, Text.literal(""));
		addSelectableChild(itemNameField);
		itemNameField.setMaxLength(256);
		
		addDrawableChild(
			addButton = ButtonWidget.builder(Text.literal("Add"), b -> {
				itemList.add(itemToAdd);
				itemNameField.setText("");
			}).dimensions(width / 2 - 2, height - 56, 30, 20).build());
		
		addDrawableChild(removeButton = ButtonWidget
			.builder(Text.literal("Remove Selected"),
				b -> itemList.remove(listGui.selected))
			.dimensions(width / 2 + 52, height - 56, 100, 20).build());
		
		addDrawableChild(ButtonWidget.builder(Text.literal("Reset to Defaults"),
			b -> client.setScreen(new ConfirmScreen(b2 -> {
				if(b2)
					itemList.resetToDefaults();
				client.setScreen(EditItemListScreen.this);
			}, Text.literal("Reset to Defaults"),
				Text.literal("Are you sure?"))))
			.dimensions(width - 108, 8, 100, 20).build());
		
		addDrawableChild(doneButton = ButtonWidget
			.builder(Text.literal("Done"), b -> client.setScreen(prevScreen))
			.dimensions(width / 2 - 100, height - 28, 200, 20).build());
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		boolean childClicked = super.mouseClicked(mouseX, mouseY, mouseButton);
		
		itemNameField.mouseClicked(mouseX, mouseY, mouseButton);
		listGui.mouseClicked(mouseX, mouseY, mouseButton);
		
		if(!childClicked && (mouseX < (width - 220) / 2
			|| mouseX > width / 2 + 129 || mouseY < 32 || mouseY > height - 64))
			listGui.selected = -1;
		
		return childClicked;
	}
	
	@Override
	public boolean mouseDragged(double double_1, double double_2, int int_1,
		double double_3, double double_4)
	{
		listGui.mouseDragged(double_1, double_2, int_1, double_3, double_4);
		return super.mouseDragged(double_1, double_2, int_1, double_3,
			double_4);
	}
	
	@Override
	public boolean mouseReleased(double double_1, double double_2, int int_1)
	{
		listGui.mouseReleased(double_1, double_2, int_1);
		return super.mouseReleased(double_1, double_2, int_1);
	}
	
	@Override
	public boolean mouseScrolled(double double_1, double double_2,
		double double_3)
	{
		listGui.mouseScrolled(double_1, double_2, double_3);
		return super.mouseScrolled(double_1, double_2, double_3);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		switch(keyCode)
		{
			case GLFW.GLFW_KEY_ENTER:
			if(addButton.active)
				addButton.onPress();
			break;
			
			case GLFW.GLFW_KEY_DELETE:
			if(!itemNameField.isFocused())
				removeButton.onPress();
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			doneButton.onPress();
			break;
			
			default:
			break;
		}
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void tick()
	{
		itemNameField.tick();
		
		itemToAdd = ItemUtils
			.getItemFromNameOrID(itemNameField.getText().toLowerCase());
		addButton.active = itemToAdd != null;
		
		removeButton.active =
			listGui.selected >= 0 && listGui.selected < listGui.list.size();
	}
	
	@Override
	public void render(DrawContext helper, int mouseX, int mouseY,
		float partialTicks)
	{
		MatrixStack matrixStack = helper.getMatrices();
		listGui.render(helper, mouseX, mouseY, partialTicks);
		
		helper.drawCenteredTextWithShadow(client.textRenderer,
			itemList.getName() + " (" + listGui.getItemCount() + ")", width / 2,
			12, 0xffffff);
		
		matrixStack.push();
		matrixStack.translate(0, 0, 300);
		
		itemNameField.render(helper, mouseX, mouseY, partialTicks);
		super.render(helper, mouseX, mouseY, partialTicks);
		
		matrixStack.translate(-64 + width / 2 - 152, 0, 0);
		
		if(itemNameField.getText().isEmpty() && !itemNameField.isFocused())
		{
			matrixStack.push();
			matrixStack.translate(0, 0, 300);
			helper.drawTextWithShadow(client.textRenderer, "item name or ID",
				68, height - 50, 0x808080);
			matrixStack.pop();
		}
		
		int border = itemNameField.isFocused() ? 0xffffffff : 0xffa0a0a0;
		int black = 0xff000000;
		
		helper.fill(48, height - 56, 64, height - 36, border);
		helper.fill(49, height - 55, 64, height - 37, black);
		helper.fill(214, height - 56, 244, height - 55, border);
		helper.fill(214, height - 37, 244, height - 36, border);
		helper.fill(244, height - 56, 246, height - 36, border);
		helper.fill(214, height - 55, 243, height - 52, black);
		helper.fill(214, height - 40, 243, height - 37, black);
		helper.fill(215, height - 55, 216, height - 37, black);
		helper.fill(242, height - 55, 245, height - 37, black);
		
		matrixStack.pop();
		
		RenderUtils.drawItem(helper,
			itemToAdd == null ? ItemStack.EMPTY : new ItemStack(itemToAdd),
			width / 2 - 164, height - 52, false);
	}
	
	@Override
	public boolean shouldPause()
	{
		return false;
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	private static class ListGui extends ListWidget
	{
		private final MinecraftClient mc;
		private final List<String> list;
		private int selected = -1;
		
		public ListGui(MinecraftClient mc, EditItemListScreen screen,
			List<String> list)
		{
			super(mc, screen.width, screen.height, 32, screen.height - 64, 30);
			this.mc = mc;
			this.list = list;
		}
		
		@Override
		protected int getItemCount()
		{
			return list.size();
		}
		
		@Override
		protected boolean selectItem(int index, int int_2, double var3,
			double var4)
		{
			if(index >= 0 && index < list.size())
				selected = index;
			
			return true;
		}
		
		@Override
		protected boolean isSelectedItem(int index)
		{
			return index == selected;
		}
		
		@Override
		protected void renderBackground()
		{
			
		}
		
		@Override
		protected void renderItem(DrawContext helper, int index, int x,
			int y, int var4, int var5, int var6, float partialTicks)
		{
			String name = list.get(index);
			Item item = Registries.ITEM.get(new Identifier(name));
			ItemStack stack = new ItemStack(item);
			TextRenderer tr = mc.textRenderer;
			
			RenderUtils.drawItem(helper, stack, x + 1, y + 1, true);
			String displayName = stack.isEmpty() ? "\u00a7ounknown item\u00a7r"
				: stack.getName().getString();
			helper.drawText(tr, displayName, x + 28, y, 0xf0f0f0, false);
			helper.drawText(tr, name, x + 28, y + 9, 0xa0a0a0, false);
			helper.drawText(tr, "ID: " + Registries.ITEM.getRawId(item),
				x + 28, y + 18, 0xa0a0a0, false);
		}
	}
}
