package yadokaris_Youtube_Chat_Viewer;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.kusaanko.youtubelivechat.AuthorType;
import com.github.kusaanko.youtubelivechat.ChatItem;
import com.github.kusaanko.youtubelivechat.ChatItemType;
import com.github.kusaanko.youtubelivechat.IdType;
import com.github.kusaanko.youtubelivechat.YouTubeLiveChat;
import com.google.gson.Gson;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.MinecraftForgeClient;

public class ConfigCommand implements ICommand, Runnable {

	static String channelID = "";
	private static String liveID = "";
	private static boolean isClientMode = true;
	private static boolean isView = false;
	static YouTubeLiveChat chat = null;
	private static long startTime = 0;
	static boolean isLogin = false;
	private static String id;
	private static boolean isTopOnly = true;
	private static IdType type;
	private static Map<Color, String> colors = new HashMap<Color, String>() {{
		put(new Color(0x000000), "black");
		put(new Color(0x000080), "dark_blue");
		put(new Color(0x008000), "dark_green");
		put(new Color(0x008080), "dark_aqua");
		put(new Color(0x800000), "dark_red");
		put(new Color(0x800080), "dark_purple");
		put(new Color(0xFF6600), "gold");
		put(new Color(0xC0C0C0), "gray");
		put(new Color(0x808080), "dark_gray");
		put(new Color(0x0000FF), "blue");
		put(new Color(0x00FF00), "green");
		put(new Color(0x00FFFF), "aqua");
		put(new Color(0xFF0000), "red");
		put(new Color(0xFF00FF), "light_purple");
		put(new Color(0xFFFF00), "yellow");
		put(new Color(0xFFFFFF), "white");
	}};

	@Override
	public int compareTo(ICommand o) {
		return 0;
	}

	@Override
	public String getName() {
		return "ycv";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/ycv < channel | live | start | end > < channel id | live id | mode > <id type> <is top chat>";
	}

	@Override
	public List<String> getAliases() {
		List<String> list = new ArrayList<>();
		list.add("ycv");
		return list;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		try {
			if (args[0].equals("stop")) {
				isView = false;
				chat = null;
				sender.sendMessage(new TextComponentString("Chat stopped").setStyle(new Style().setColor(TextFormatting.GREEN)));
			}
			else if (args[0].equals("channel")) {
				YCV.prop.setProperty("channelID", args[1]);
				channelID = args[1];
				try {
					YCV.saveProperties();
				}
				catch (IOException e) {
					e.printStackTrace();
					sender.sendMessage(new TextComponentString("Failed to set the channel").setStyle(new Style().setColor(TextFormatting.RED)));
					return;
				}
				sender.sendMessage(new TextComponentString("Set the channel").setStyle(new Style().setColor(TextFormatting.GREEN)));
			}
			else if (args[0].equals("live")) {
				liveID = args[1];
				sender.sendMessage(new TextComponentString("Set the live").setStyle(new Style().setColor(TextFormatting.GREEN)));
			}
			else if (args[0].equals("start")) {
				isView = false;
				if (args[1].equals("client")) isClientMode = true;
				else if (args[1].equals("multi")) isClientMode = false;
				else {
					sender.sendMessage(new TextComponentString("Invalid argument").setStyle(new Style().setColor(TextFormatting.RED)));
					return;
				}

				if (args[2].equals("channel")) {
					id = channelID;
					type = IdType.CHANNEL;
				}
				else if (args[2].equals("live")) {
					id = liveID;
					type = IdType.VIDEO;
				}
				else {
					sender.sendMessage(new TextComponentString("Invalid argument").setStyle(new Style().setColor(TextFormatting.RED)));
					return;
				}

				if (args[3].equals("true")) isTopOnly = true;
				else if (args[3].equals("false")) isTopOnly = false;
				else {
					sender.sendMessage(new TextComponentString("Invalid argument").setStyle(new Style().setColor(TextFormatting.RED)));
					return;
				}

				try {
					chat = new YouTubeLiveChat(id, isTopOnly, type);
					chat.setLocale(MinecraftForgeClient.getLocale());
					if(isLogin) chat.setUserData(YCV.cookie.getIDs());
					isView = true;
				}
				catch (IOException | IllegalArgumentException e) {
					sender.sendMessage(new TextComponentString("Invalid ID").setStyle(new Style().setColor(TextFormatting.RED)));
					e.printStackTrace();
					return;
				}

				startTime = System.currentTimeMillis();

				new Thread(this).start();
				sender.sendMessage(new TextComponentString("Chat started").setStyle(new Style().setColor(TextFormatting.GREEN)));
			}
			else if (args[0].equals("restart")) {
				chat = null;
				isView = false;

				try {
					chat = new YouTubeLiveChat(id, isTopOnly, type);
					chat.setLocale(MinecraftForgeClient.getLocale());
					if(isLogin) chat.setUserData(YCV.cookie.getIDs());
					isView = true;
				}
				catch (IOException | IllegalArgumentException e) {
					sender.sendMessage(new TextComponentString("Invalid ID").setStyle(new Style().setColor(TextFormatting.RED)));
					e.printStackTrace();
					return;
				}

				startTime = System.currentTimeMillis();

				new Thread(this).start();
				sender.sendMessage(new TextComponentString("Chat restarted").setStyle(new Style().setColor(TextFormatting.GREEN)));
			}
			else if (args[0].equals("login")) {
				try {
					YCV.cookie = new YoutubeCookie(Browser.getBrowser(args[1]));
					if (!YCV.cookie.hasAllIds()) {
						YCV.cookie = null;
						sender.sendMessage(new TextComponentString("Failed to get authorization info").setStyle(new Style().setColor(TextFormatting.RED)));
						sender.sendMessage(new TextComponentString("Please login with the browser").setStyle(new Style().setColor(TextFormatting.RED)));
						return;
					}
					if (isView) chat.setUserData(YCV.cookie.getIDs());
					startTime = System.currentTimeMillis();
				}
				catch (IllegalArgumentException e) {
					sender.sendMessage(new TextComponentString("Invalid browser name").setStyle(new Style().setColor(TextFormatting.RED)));
				}

				YCV.prop.setProperty("browser", args[1].toLowerCase());
				try {
					YCV.saveProperties();
				}
				catch (IOException e) {
					e.printStackTrace();
					sender.sendMessage(new TextComponentString("Failed to set the browser").setStyle(new Style().setColor(TextFormatting.RED)));
					return;
				}

				isLogin = true;
				sender.sendMessage(new TextComponentString("Logged in").setStyle(new Style().setColor(TextFormatting.GREEN)));
			}
			else if (args[0].equals("logout")) {
				YCV.cookie = null;

				YCV.prop.setProperty("browser", "none");
				try {
					YCV.saveProperties();
				}
				catch (IOException e) {
					e.printStackTrace();
					sender.sendMessage(new TextComponentString("Failed to save").setStyle(new Style().setColor(TextFormatting.RED)));
					return;
				}

				isLogin = false;
				sender.sendMessage(new TextComponentString("Logged out").setStyle(new Style().setColor(TextFormatting.GREEN)));
			}
			else if (args[0].equals("c")) {
				if (!isView) {
					sender.sendMessage(new TextComponentString("You can send a message while displaying chat").setStyle(new Style().setColor(TextFormatting.RED)));
					sender.sendMessage(new TextComponentString("Please start displaying chat using /ycv start <mode> <type> <isTopOnly>").setStyle(new Style().setColor(TextFormatting.RED)));
					return;
				}
				if (!isLogin || YCV.cookie == null) {
					sender.sendMessage(new TextComponentString("You need to login using /ycv login <browser>").setStyle(new Style().setColor(TextFormatting.RED)));
					return;
				}

				new Thread(() -> {
					try {
						StringBuilder sb = new StringBuilder();
						for (int i = 1; i < args.length; i++) {
							sb.append(args[i] + " ");
						}
						System.out.println(sb.toString());
						chat.sendMessage(sb.toString());
					}
					catch (IllegalStateException | IOException e) {
						e.printStackTrace();
						sender.sendMessage(new TextComponentString("Failed to send the message").setStyle(new Style().setColor(TextFormatting.RED)));
					}
				}).start();
			}
			else if (args[0].equals("test")) {
				sender.sendMessage(new TextComponentString(args[1]));
				System.out.println(args[1]);
			}
			else {
				sender.sendMessage(new TextComponentString("Invalid argument").setStyle(new Style().setColor(TextFormatting.RED)));
				return;
			}
		}
		catch (IndexOutOfBoundsException e) {
			sender.sendMessage(new TextComponentString("Invalid argument").setStyle(new Style().setColor(TextFormatting.RED)));
			e.printStackTrace();
		}
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return true;
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
			BlockPos targetPos) {
		return null;
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return false;
	}

	public static String getClosestColor(Color color) {
		String result = "";
		double min = Integer.MAX_VALUE;

		for (Color c : colors.keySet()) {
			double dist = Math.sqrt(Math.pow(c.getRed() - color.getRed(), 2) + Math.pow(c.getGreen() - color.getGreen(), 2) + Math.pow(c.getBlue() - color.getBlue(), 2));
			if (min >= dist) {
				min = dist;
				result = colors.get(c);
			}
		}

		return result;
	}

	@Override
	public void run() {
		while (isView && chat != null) {
			try {
				chat.update();
			}
			catch (IOException e1) {
				e1.printStackTrace();
			}

			for (ChatItem item : chat.getChatItems()) {
				if (item.getTimestamp() / 1000 < startTime) continue;
				if (item.getType() == ChatItemType.TICKER_PAID_MESSAGE) continue;
				//if (item.getType() != ChatItemType.PAID_MESSAGE) continue;
				if (item.getAuthorType().contains(AuthorType.YOUTUBE)) continue;
				List<AuthorType> authorTypes = item.getAuthorType();
				ChatItemType chatType = item.getType();
				String name = item.getAuthorName() == null ? "Error" : item.getAuthorName();
				String message = item.getMessage() == null ? "" : item.getMessage();
				if (chatType == ChatItemType.PAID_STICKER) message = "Super Sticker";
				if (chatType == ChatItemType.NEW_MEMBER_MESSAGE || chatType == ChatItemType.PAID_MESSAGE || chatType == ChatItemType.PAID_STICKER) {
					SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
					handler.playSound(PositionedSoundRecord.getRecord(new SoundEvent(new ResourceLocation("minecraft", "entity.player.levelup")), 0.5F, 0.3F));
				}

				if (isClientMode) {
					ITextComponent text = new TextComponentString("<");

					ITextComponent nameComponent = new TextComponentString(name);
					Style nameStyle = new Style();
					if (authorTypes.contains(AuthorType.OWNER)) nameStyle.setColor(TextFormatting.GOLD);
					else if (authorTypes.contains(AuthorType.MODERATOR)) nameStyle.setColor(TextFormatting.BLUE);
					else if (authorTypes.contains(AuthorType.MEMBER)) nameStyle.setColor(TextFormatting.GREEN);
					else nameStyle.setColor(TextFormatting.WHITE);
					text.appendSibling(nameComponent.setStyle(nameStyle)).appendSibling(new TextComponentString("> "));

					ITextComponent messageComponent = new TextComponentString(message);
					Style messageStyle = new Style();
					if (chatType == ChatItemType.PAID_MESSAGE) {
						messageStyle.setColor(TextFormatting.getValueByName(getClosestColor(new Color(item.getBodyBackgroundColor()))));
						text.appendSibling(messageComponent.setStyle(messageStyle));
						text.appendSibling(new TextComponentString(" (" + item.getPurchaseAmount() + ")").setStyle(messageStyle));
					}
					else if (chatType == ChatItemType.PAID_STICKER) {
						messageStyle.setColor(TextFormatting.getValueByName(getClosestColor(new Color(item.getBackgroundColor()))));
						text.appendSibling(messageComponent.setStyle(messageStyle));
						text.appendSibling(new TextComponentString(" (" + item.getPurchaseAmount() + ")").setStyle(messageStyle));
					}
					else text.appendSibling(messageComponent);

					YCV.player.sendMessage(text);
				}
				else {
					Map<String, String> temp = new HashMap<>();
					StringBuilder sb = new StringBuilder("/tellraw @a [");
					Gson gson = new Gson();

					temp.put("text", "<");
					temp.put("color", "white");
					sb.append(gson.toJson(temp) + ", ");

					temp.put("text", name);
					if (authorTypes.contains(AuthorType.OWNER)) temp.put("color", "gold");
					else if (authorTypes.contains(AuthorType.MODERATOR)) temp.put("color", "blue");
					else if (authorTypes.contains(AuthorType.MEMBER)) temp.put("color", "green");
					else temp.put("color", "white");
					sb.append(gson.toJson(temp) + ", ");
					temp.put("text", "> ");
					temp.put("color", "white");
					sb.append(gson.toJson(temp) + ", ");

					temp.put("text", message);
					if (chatType == ChatItemType.PAID_MESSAGE) {
						temp.put("color", getClosestColor(new Color(item.getBodyBackgroundColor())));
						sb.append(gson.toJson(temp) + ", ");
						temp.put("text", " (" + item.getPurchaseAmount() + ")");
						temp.put("color", getClosestColor(new Color(item.getBodyBackgroundColor())));
						sb.append(gson.toJson(temp) + ", ");
					}
					else if (chatType == ChatItemType.PAID_STICKER) {
						temp.put("color", getClosestColor(new Color(item.getBackgroundColor())));
						sb.append(gson.toJson(temp) + ", ");
						temp.put("text", " (" + item.getPurchaseAmount() + ")");
						temp.put("color", getClosestColor(new Color(item.getBackgroundColor())));
						sb.append(gson.toJson(temp) + ", ");
					}
					else {
						temp.put("color", "white");
						sb.append(gson.toJson(temp) + ", ");
					}

					sb.delete(sb.length() - 2, sb.length());
					sb.append("]");

					if (sb.length() >= 256) sb = new StringBuilder("/tellraw @a {\"text\":\"This message is too long!\", \"color\":\"red\"}");
					((EntityPlayerSP)YCV.player).sendChatMessage(sb.toString());
				}
			}

			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
