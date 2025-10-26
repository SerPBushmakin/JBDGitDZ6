import java.util.concurrent.atomic.AtomicBoolean;

// Кассир: обработчик транзакций
class Cashier implements Runnable {
    private final Bank bank;
    private final AtomicBoolean active;

    public Cashier(Bank bank, AtomicBoolean active) {
        this.bank = bank;
        this.active = active;
    }

    @Override
    public void run() {
        try {
            while (active.get() || !bank.isTransactionQueueEmpty()) {
                Transaction tx = bank.getTransaction();
                if (tx != null) {
                    tx.process(bank);
                }
            }
        } catch (InterruptedException e) {
            // Поток прерван — завершение
            Thread.currentThread().interrupt();
        }
    }
}

