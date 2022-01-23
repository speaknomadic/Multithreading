import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class AccountWithConditionsUser {
	private static Account account = new Account();
	public static void main(String[] args) {
		System.out.println("Thread 1\t\tThread 2\t\tBalance");
		/**
		 * Create new thread pool with two threads
		 */
		ExecutorService executor = Executors.newFixedThreadPool(2);
		executor.execute(new DepositTask());
		executor.execute(new WithdrawTask());
		executor.shutdown();
		/**
		 * Keep this program going as long as it is not shut down
		 */
		while(!executor.isShutdown()) {
		}
	}
	public static class DepositTask implements Runnable{
		public void run() {
			try {
				while(true) {
					/**
					 * We do not want to withdraw more money than we have in the account.
					 * The deposit amount is a random number between 1 and 10
					 */
					account.deposit((int)(Math.random()*10)+1);
					/**
					 * By putting the thread to sleep we make sure that all threads have time to execute. 
					 * Make sure there is balance between the two threads
					 */
					Thread.sleep(1000);
				}
			}
			catch(InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static class WithdrawTask implements Runnable{
		public void run() {
			/**
			 * Similar to the DepositTask, without the check for balance.
			 */
			while(true) {
				account.withdraw((int)(Math.random() * 10) + 1);
			}
		}
	}
	
	private static class Account{
		/**
		 * Create and acquire a Lock, with a fairness policy of true
		 */
		private static Lock lock = new ReentrantLock(true);
		/**
		 * Create a Condition how we actually are going to pass this Lock. 
		 * Pass this condition only if there is the possibility of withdraw happening
		 *
		 */
		private static Condition newDeposit = lock.newCondition();
		private int balance = 0;
		
		public int getBalance() {
			return balance;
		}
		
		public void withdraw(int amount) {
			/**
			 * Acquire a lock
			 */
			lock.lock();
			try {
				/**
				 * We withdraw only if there is enough balance in the account
				 */
				while(balance<amount) {
					/**
					 * Print there is not enough money for withdraw.
					 */
					System.out.println("\t\t\tWait for deposit");
					/**
					 * newdeposit now is a condition which is false. 
					 * It is signaling the newdeposit condition that a deposit is needed.
					 * It will keep running this until it gets enough money
					 */
					newDeposit.await();
				}
				/**
				 * Once the while condition evaluates to true the aout is extracted from the balance
				 */
				balance-=amount;
				/**
				 * Print withdraw was successful
				 *
				 */
				System.out.println("\t\t\tWithdraw " +amount+ "\t\t"+ getBalance());
			}
			/**
			 * If the thread is interrupted, an exception might be thrown
			 */
			catch(InterruptedException ex) {
				ex.printStackTrace();
			}
			finally {
				/**
				 * release the lock
				 */
				lock.unlock();
			}
		}
		
		public void deposit(int amount) {
			/**
			 * Acquire a lock
			 */
			lock.lock();
			try {
				balance = balance+amount;
				/**
				 * Print the deposit amount
				 */
				System.out.println("Deposit "+amount+"\t\t\t\t\t"+ balance);
				/**
				 * Signaling the deposit is done, the other thread can call withdraw again
				 */
				newDeposit.signalAll();
			}
			finally {
				/**
				 * release the lock
				 */
				lock.unlock();
			}
		}
		
	}
}
