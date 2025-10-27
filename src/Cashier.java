import java.util.concurrent.atomic.AtomicBoolean;

// Кассир: обработчик транзакций
class Cashier implements Runnable {
    private final Bank bank;
    private final AtomicBoolean active;// Ссылка на флаг активности банка

    public Cashier(Bank bank, AtomicBoolean active) {
        this.bank = bank;
        this.active = active;
    }

    @Override
    public void run() {
        try {
            // Цикл продолжается, пока банк активен ИЛИ пока в очереди есть транзакции.
            // Это гарантирует, что все оставшиеся транзакции будут обработаны даже после установки isActive в false.
            while (active.get() || !bank.isTransactionQueueEmpty()) {
                Transaction tx = bank.getTransaction();// Пытаемся получить транзакцию
                if (tx != null) {
                    tx.process(bank);// Если транзакция получена, обрабатываем ее
                }
            }
        } catch (InterruptedException e) {
            // Поток прерван — завершение
            Thread.currentThread().interrupt();
        }
    }
}

