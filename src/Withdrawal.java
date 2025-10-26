// Снятие
class Withdrawal extends Transaction {
    final double amount;

    public Withdrawal(int clientId, double amount) {
        super(clientId);
        this.amount = amount;
    }

    @Override
    public void process(Bank bank) {
        bank.withdraw(clientId, amount);
        bank.notifyObservers("Withdraw: клиент #" + clientId + " снял " + amount);
    }
}
