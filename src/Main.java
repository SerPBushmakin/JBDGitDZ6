
// основной пользовательский интерфейс
public class Main {
    public static void main(String[] args) throws InterruptedException {
        Bank bank = new Bank(3);
        // Регистрация логгера
        bank.addObserver(new Logger());

        // Создаем клиентов
        bank.addClient(new Client(1, 1000, "USD"));
        bank.addClient(new Client(2, 2000, "EUR"));

        // Проверка баланса
        System.out.println("Клиент 1: " + bank.getClient(1).getBalance() + " " + bank.getClient(1).getCurrency());
        System.out.println("Клиент 2: " + bank.getClient(2).getBalance() + " " + bank.getClient(2).getCurrency());


        // Добавляем транзакции
        bank.enqueueTransaction(new Deposit(1, 500));// Клиент 1 пополняет на 500
        bank.enqueueTransaction(new Withdrawal(2, 300));// Клиент 2 снимает 300
        bank.enqueueTransaction(new CurrencyExchange(1, "USD", "RUB", 100));// Клиент 1 меняет 100 USD на RUB
        bank.enqueueTransaction(new FundsTransfer(1, 2, 200));// Клиент 1 переводит 200 клиенту 2

        // Даем время обработать транзакции
        Thread.sleep(20000);

        // Проверка баланса
        System.out.println("Клиент 1: " + bank.getClient(1).getBalance() + " " + bank.getClient(1).getCurrency());
        System.out.println("Клиент 2: " + bank.getClient(2).getBalance() + " " + bank.getClient(2).getCurrency());

        // Остановка банка и потоков
        bank.shutdown();

        System.out.println("Работа завершена.");
    }
}
