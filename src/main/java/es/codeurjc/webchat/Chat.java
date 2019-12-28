package es.codeurjc.webchat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class Chat {

	private String name;
	private Map<String, User> users = new ConcurrentHashMap<>();

	private ChatManager chatManager;

	public Chat(ChatManager chatManager, String name) {
		this.chatManager = chatManager;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addUser(User user) {
		users.putIfAbsent(user.getName(), user);
		for (User u : users.values()) {
			if (u != user) {
				u.newUserInChat(this, user);
			}
		}

	}

	public void removeUser(User user) {
		users.remove(user.getName());
		for (User u : users.values()) {
			u.userExitedFromChat(this, user);
		}
	}

	public Collection<User> getUsers() {
		return Collections.unmodifiableCollection(users.values());
	}

	public User getUser(String name) {
		return users.get(name);
	}

	public void sendMessage(User user, String message) {
		List<Thread> threads = new ArrayList<Thread>(users.size());
		CountDownLatch cdl = new CountDownLatch(users.size());
		for (User u : users.values()) {
			Thread t = new Thread(() -> {
				u.newMessage(this, user, message);
				cdl.countDown();
			});
			threads.add(t);
			t.start();
		}
		try {
			cdl.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		this.chatManager.closeChat(this);
	}

	public void printUsers() {
		System.out.println("Usuarios " + this.name);
		for (User u : users.values()) {
			System.out.println(this.name + " - " + u.getName());
		}

	}

}
