package yadokaris_Youtube_Chat_Viewer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = "yadokaris_youtube_chat_viewer", name = "yadokari's Youtube Chat Viewer", version = YCV.version, updateJSON = "https://raw.githubusercontent.com/yadokari1130/Youtube-Chat-Viewer/master/update.json")
public class YCV {

	static String playerName;
	static EntityPlayer player;
	static final String version = "1.0";
	static String path;
	static Properties prop = new Properties();
	private static boolean isNotificated = false;
	static Browser browser;
	static YoutubeCookie cookie;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		path = event.getSuggestedConfigurationFile().getParent() + "\\YCV.properties";

		prop = new Properties();
		try (InputStream reader = new FileInputStream(path)) {
			prop.load(reader);
		}
		catch (FileNotFoundException e) {
			try (OutputStream writer = new FileOutputStream(path)) {
				prop.store(writer, "comments");
				writer.flush();
			}
			catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		if (prop.containsKey("channelID")) ConfigCommand.channelID = prop.getProperty("channelID");
		if (prop.containsKey("browser") && !prop.get("browser").equals("none")) {
			browser = Browser.getBrowser(prop.getProperty("browser"));
			cookie = new YoutubeCookie(browser);
			ConfigCommand.isLogin = true;
		}

		ClientCommandHandler.instance.registerCommand(new ConfigCommand());
		System.out.println(System.getProperty("file.encoding"));
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	public static void saveProperties() throws IOException {
		OutputStream writer = new FileOutputStream(path);
		prop.store(writer, "comments");
		writer.flush();
	}

	@SuppressWarnings("unchecked")
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onJoinWorld(EntityJoinWorldEvent event) {
		if (Minecraft.getMinecraft().player != null && event.getEntity().equals(Minecraft.getMinecraft().player)) {
			EntityPlayer player = Minecraft.getMinecraft().player;
			this.player = player;
			this.playerName = player.getName();

			new Thread(() -> {
				String update = null;
				try {
					update = IOUtils.toString(new URL("https://raw.githubusercontent.com/yadokari1130/Youtube-Chat-Viewer/master/update.json"), "UTF-8");
				}
				catch (IOException e) {
					e.printStackTrace();
				}

				Gson gson = new Gson();
				Map map = gson.fromJson(update, Map.class);

				if (map.isEmpty()) return;

				String latest = ((Map<String, String>) map.get("promos")).get("1.12.2-latest");

				if (!isNotificated && !latest.equals(version)) {
					new Thread(() -> {
						try {
							Thread.sleep(5000);
						}
						catch (InterruptedException e) {
							e.printStackTrace();
						}
						ClickEvent linkClickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, (String)map.get("homepage"));
						Style clickableStyle = new Style().setClickEvent(linkClickEvent).setColor(TextFormatting.AQUA);
						Style color = new Style().setColor(TextFormatting.GREEN);
						player.sendMessage(new TextComponentTranslation("yadokaris_ycv.update.message1").setStyle(color));
						player.sendMessage(new TextComponentTranslation("yadokaris_ycv.update.message2").setStyle(clickableStyle));
						player.sendMessage(new TextComponentTranslation("yadokaris_ycv.update.infomation"));
						player.sendMessage(new TextComponentString("----------------------------------------------------------------------"));
						player.sendMessage(new TextComponentString(((Map<String, String>) map.get("1.12.2")).get(latest)));
						player.sendMessage(new TextComponentString("----------------------------------------------------------------------"));
					}).start();
					isNotificated = true;
				}
			}).start();
		}
	}
}