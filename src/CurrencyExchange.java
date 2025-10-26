// Обмен валют
class CurrencyExchange extends Transaction {
    final String fromCurrency;
    final String toCurrency;
    final double amount;

    public CurrencyExchange(int clientId, String fromCurrency, String toCurrency, double amount) {
        super(clientId);
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.amount = amount;
    }

    @Override
    public void process(Bank bank) {
        bank.exchangeCurrency(clientId, fromCurrency, toCurrency, amount);
        bank.notifyObservers("Exchange: клиент #" + clientId + " обменял " + amount + " " + fromCurrency + " на " + toCurrency);
    }
}
