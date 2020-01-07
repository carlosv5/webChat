package es.codeurjc.webchat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class Chat {

	private String name;
	private Map<String, User> users = new ConcurrentHashMap<>();
	private Map<User, Queue<String>> userMessages = new ConcurrentHashMap<>();
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
		userMessages.putIfAbsent(user, new LinkedList<String>());
		List<Thread> threads = new ArrayList<Thread>(users.size());
		for (User u : users.values()) {
			if (u != user) {
				Thread t = new Thread(() -> {
					u.newUserInChat(this, user);
				});
				threads.add(t);
			}
		}
		for (Thread th : threads) {
			th.start();
		}
		for (Thread th : threads) {
			try {
				th.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			this.userMessages.get(u).add(message);
		}

		for (User u : users.values()) {

			Thread t = new Thread(() -> {
				u.newMessage(this, user, userMessages.get(u).poll());
				cdl.countDown();
			});
			threads.add(t);
		}
		for (Thread th : threads) {
			th.start();
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
		List<Thread> threads = new ArrayList<Thread>(users.size());
		for (User u : users.values()) {
			Thread t = new Thread(() -> {
				System.out.println(this.name + " - " + u.getName());
			});
			threads.add(t);
		}
		for(Thread th : threads) {
			th.start();
		}
		for(Thread th : threads) {
			try {
				th.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
