// Перевод между клиентами
class FundsTransfer extends Transaction {
    final int receiverId;
    final double amount;

    public FundsTransfer(int senderId, int receiverId, double amount) {
        super(senderId);
        this.receiverId = receiverId;
        this.amount = amount;
    }

    @Override
    public void process(Bank bank) {
        bank.transferFunds(this.clientId, receiverId, amount);
        bank.notifyObservers("Transfer: клиент #" + clientId + " перевел " + amount + " клиенту #" + receiverId);
    }
}
