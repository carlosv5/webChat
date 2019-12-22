package es.sidelab.webchat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;
import es.codeurjc.webchat.User;

public class ChatManagerTest {

	@Test
	public void newChat() throws InterruptedException, TimeoutException {

		// Crear el chat Manager
		ChatManager chatManager = new ChatManager(5);

		// Crear un usuario que guarda en chatName el nombre del nuevo chat
		final String[] chatName = new String[1];

		chatManager.newUser(new TestUser("user") {
			public void newChat(Chat chat) {
				chatName[0] = chat.getName();
			}
		});

		// Crear un nuevo chat en el chatManager
		chatManager.newChat("Chat", 5, TimeUnit.SECONDS);

		// Comprobar que el chat recibido en el método 'newChat' se llama 'Chat'
		assertTrue("The method 'newChat' should be invoked with 'Chat', but the value is "
		                + chatName[0], Objects.equals(chatName[0], "Chat"));
	}

	@Test
	public void newUserInChat() throws InterruptedException, TimeoutException {

		ChatManager chatManager = new ChatManager(5);

		final String[] newUser = new String[1];

		TestUser user1 = new TestUser("user1") {
			@Override
			public void newUserInChat(Chat chat, User user) {
				newUser[0] = user.getName();
			}
		};

		TestUser user2 = new TestUser("user2");

		chatManager.newUser(user1);
		chatManager.newUser(user2);

		Chat chat = chatManager.newChat("Chat", 5, TimeUnit.SECONDS);

		chat.addUser(user1);
		chat.addUser(user2);

		assertTrue("Notified new user '" + newUser[0] + "' is not equal than user name 'user2'",
				"user2".equals(newUser[0]));

	}

	private void threadTask(String threadNumber, ChatManager chatM) throws InterruptedException, TimeoutException {
		TestUser user = new TestUser(threadNumber);
		chatM.newUser(user);
		for (int i = 0; i < 5; i++) {
			String chatName = "chat " + i;
			chatM.newChat(chatName, 5, TimeUnit.SECONDS);
			chatM.getChat(chatName).addUser(user);
			chatM.getChat(chatName).printUsers();
		}
	}

	@Test
	public void concurrentChatManager50Chats4Threads() throws InterruptedException, TimeoutException {

		// Crear el chat Manager
		ChatManager chatManager = new ChatManager(50);

		// Crear hilos
		Thread t = new Thread(() -> {
			try {
				threadTask("0", chatManager);
			} catch (InterruptedException | TimeoutException | ConcurrentModificationException e) {
				e.printStackTrace();
				fail("\nError in thread 0");
			}
		});

		Thread t2 = new Thread(() -> {
			try {
				threadTask("1", chatManager);
			} catch (InterruptedException | TimeoutException | ConcurrentModificationException e) {
				e.printStackTrace();
				fail("\nError in thread 1");
			}
		});

		Thread t3 = new Thread(() -> {
			try {
				threadTask("2", chatManager);
			} catch (InterruptedException | TimeoutException | ConcurrentModificationException e) {
				e.printStackTrace();
				fail("\nError in thread 2");
			}
		});

		Thread t4 = new Thread(() -> {
			try {
				threadTask("3", chatManager);
			} catch (InterruptedException | TimeoutException | ConcurrentModificationException e) {
				e.printStackTrace();
				fail("\nError in thread 3");
			}
		});

		// Añadir hilos a la lista
		ArrayList<Thread> threads = new ArrayList<Thread>(4);
		threads.add(t);
		threads.add(t2);
		threads.add(t3);
		threads.add(t4);

		// Arrancar todos los hilos
		for (Thread th : threads) {
			th.start();
		}

		// Join para evitar que el test acabe hasta que todos los hilos hayan terminado
		for (Thread th : threads) {
			th.join();
		}

		// Comprobar que el cada chat contiene 4 usuarios
		for (Chat chat : chatManager.getChats()) {
			assertTrue("The chat " + chat.getName() + " should have 4 users but some of them are missing.",
					Objects.equals(chat.getUsers().size(), 4));
		}

	}

}
