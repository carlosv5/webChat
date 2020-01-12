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
		assertTrue("The method 'newChat' should be invoked with 'Chat', but the value is " + chatName[0],
				Objects.equals(chatName[0], "Chat"));
		assertTrue("The chat " + chatName[0] + " has not been created properly",
				Objects.nonNull(chatManager.getChat(chatName[0])));
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
	public void concurrentChatManager50Chats4Threads() throws Exception {

		// Crear el chat Manager
		ChatManager chatManager = new ChatManager(50);

		// Final array to communicate main thread if there is exception
		final Exception[] exc = new Exception[1];

		// Crear hilos
		Thread t = new Thread(() -> {
			try {
				threadTask("0", chatManager);
			} catch (InterruptedException | TimeoutException | ConcurrentModificationException e) {
				e.printStackTrace();
				exc[0] = e;
			}
		});

		Thread t2 = new Thread(() -> {
			try {
				threadTask("1", chatManager);
			} catch (InterruptedException | TimeoutException | ConcurrentModificationException e) {
				e.printStackTrace();
				exc[0] = e;
			}
		});

		Thread t3 = new Thread(() -> {
			try {
				threadTask("2", chatManager);
			} catch (InterruptedException | TimeoutException | ConcurrentModificationException e) {
				e.printStackTrace();
				exc[0] = e;
			}
		});

		Thread t4 = new Thread(() -> {
			try {
				threadTask("3", chatManager);
			} catch (InterruptedException | TimeoutException | ConcurrentModificationException e) {
				e.printStackTrace();
				exc[0] = e;
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
			if (exc[0] != null) {
				throw exc[0];
			}
		}

		// Comprobar que el cada chat contiene 4 usuarios
		for (Chat chat : chatManager.getChats()) {
			assertTrue("The chat " + chat.getName() + " should have 4 users but some of them are missing.",
					Objects.equals(chat.getUsers().size(), 4));
		}

	}

	@Test
	public void paralelNotifications() throws InterruptedException, TimeoutException {

		// Crear el chat Manager
		ChatManager chatManager = new ChatManager(1);

		// Crear chat
		final String chatName = "random";
		chatManager.newChat(chatName, 5, TimeUnit.SECONDS);

		long tInitial, testTime;
		tInitial = System.currentTimeMillis();

		for (int i = 0; i < 4; i++) {
			TestUser user = new TestUser("user" + i) {
				@Override
				public void newMessage(Chat chat, User user, String message) {
					try {
						Thread.sleep(1000);
						super.newMessage(chat, user, message);
					} catch (InterruptedException e) {
						e.printStackTrace();
						fail("\nError in user" + user.getName());
					}
				}
			};
			chatManager.newUser(user);
			chatManager.getChat(chatName).addUser(user);
		}

		User user0 = chatManager.getUser("user0");
		chatManager.getChat(chatName).sendMessage(user0, "Hello world - I am " + user0.getName());

		testTime = System.currentTimeMillis() - tInitial;
		assert (testTime < 1500);
	}

	@Test
	public void messagesInOrder() throws InterruptedException, TimeoutException {

		// Crear el chat Manager
		ChatManager chatManager = new ChatManager(1);

		// Crear chat
		final String chatName = "random";
		chatManager.newChat(chatName, 5, TimeUnit.SECONDS);
		boolean orderedMessages = false;

		TestUser user0 = new TestUser("user0") {
			@Override
			public void newMessage(Chat chat, User user, String message) {
				if (this.name == "user1")
					System.out.println("New message '" + message + "' recieved from user " + user.getName()
					+ " in chat " + chat.getName() + ". I am " + this.name);

			}
		};
		TestUser user2 = new TestUser("user2");

		TestUser user1 = new TestUser("user1") {
			@Override
			public void newMessage(Chat chat, User user, String message) {
				try {
					Thread.sleep(500);
					if (this.name == "user1")
						System.out.println("New message '" + message + "' recieved from user " + user.getName()
						+ " in chat " + chat.getName() + ". I am " + this.name);
				} catch (InterruptedException e) {
					e.printStackTrace();
					fail("\nError in user" + user.getName());
				}
			}
		};
		chatManager.newUser(user0);
		chatManager.newUser(user1);
		chatManager.newUser(user2);
		chatManager.getChat(chatName).addUser(user0);
		chatManager.getChat(chatName).addUser(user1);

		Thread t0 = new Thread(() -> {
			for (int i = 0; i < 5; i++) {
				chatManager.getChat(chatName).sendMessage(user0, user0.getName() + " mesage " + i);
			}
		});
		t0.start();
		t0.join();

		// Hay que ver como hacerlo
		assertTrue(orderedMessages);
	}

}

