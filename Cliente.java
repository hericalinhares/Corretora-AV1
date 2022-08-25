package Avaliacao1;
import java.util.ArrayList;
import java.util.Random;

public class Cliente implements Runnable {

	private static ArrayList<Cliente> listaClientes = new ArrayList<Cliente>();		// armazena todos os clientes criados
	
	private int id;										// Identifica��o do cliente
	private double saldoLimite;							// Saldo dispon�vel para compra e venda de a��es
	private Corretora minhaCorretora;					// Corretora criada para opera��es
	private ContaCorrente minhaConta;					// Conta corrente para gestionar saldo
	
	// -------------------------------------------------//
	// CONSTRUTOR 
	public Cliente (double saldoLimite, Corretora minhaCorretora) {					
		
		this.saldoLimite = saldoLimite;					// Saldo limite para investimentos
		this.minhaCorretora = minhaCorretora;			// Define corretora da aplicacao
		
		listaClientes.add(this);						// Adiciona cliente a lista de clientes
		this.id = listaClientes.size();					// Identifica��o do cliente (somente didatico)		
		
		minhaConta = new ContaCorrente
				(id, saldoLimite, minhaCorretora);			// Cria nova conta corrente para cliente
	}
	
	// -------------------------------------------------//
	@Override
	public void run() {
		// TODO Auto-generated method stub
	//	System.out.println("THREAD CLIENTE " + id + " INICIADA \n SALDO DO CLIENTE " + id + " �: " + saldoLimite + " REAIS \n");
		
		// CLIENTES ENTRAM NA CORRETORA EM HORARIOS DIFERENTES
		int time;
		Random t = new Random();
		time = t.nextInt(300) + 200;
		
		try {
			Thread.sleep(time);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// ENQUANTO A CORRETORA EST� ABERTA:
		while(minhaCorretora.getCorretoraOn()) {	
			try {
				Thread.sleep(100);
				analiseViabilidade();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	//	System.out.println("\n\nCLIENTE " + id + " SAIU DA CORRETORA \nSALDO INICIAL: R$ " + saldoLimite + 
			//	"\nSALDO FINAL: R$"+ minhaConta.getSaldoFinal() + "\nTOTAL OPERACOES: " + minhaConta.totalMovimentacoes());
	}
	
	// -------------------------------------------------//
	// METODO ESTATICO PARA CONSULTAR TODOS OS CLIENTES
	public static ArrayList<Cliente> getListaClientes() {
		return listaClientes;
	}
	
	public void consultaCliente() {
		System.out.println("Seu saldo � "+ this.saldoLimite +" reais.");
	}
	
	// FAZ ANALISE DE VIABILIDADE DE COMPRA E VENDA DE ACOES
	private void analiseViabilidade() throws InterruptedException {
	
		ArrayList<Ativos> ativosAtuais = minhaCorretora.consultaAtivos();
		int ind = minhaCorretora.consultaAtivoAtual()-1;
		int[] viabilidade = new int[ativosAtuais.size()];
		
		// verifica melhor a��o para ser comprada
		
		for(int i=0; i < ativosAtuais.size(); i++) {
			int v1, v2, v3, v4;					// indicam pesos das analise
			
			// v1: verifica se MMC cruzou MMI
			
			if(ativosAtuais.get(i).getMediaExponencial().get(ind).getCurta() > 
				ativosAtuais.get(i).getMediaExponencial().get(ind).getIntermediaria()) {
				v1 = 1;			// em alta
			} else {
				v1 = 0;
			}
					
			// v2: verifica se MMI cruzou MML
			if(ativosAtuais.get(i).getMediaExponencial().get(ind).getIntermediaria() > 
			ativosAtuais.get(i).getMediaExponencial().get(ind).getLonga()) {
				v2 = 2;			// em alta
			} else {
				v2 = 0;
			}
			
			// v3: verifica se desvio padr�o � menor do que 70%
			if(ativosAtuais.get(i).getDesvioPadrao().get(ind) < 0.7) {
				v3 = 1;
			} else {
				v3 = 0;
			}
			
			// v4: verifica se pre�o atual est� acima ou abaixo das medias
			if(ativosAtuais.get(i).getMediaSimples().get(ind).getCurta() > 
			ativosAtuais.get(i).getMediaSimples().get(ind).getLonga()) {
				v4 = 2;
			} else {
				v4 = 0;
			}
			
			viabilidade[i] = v1 + v2 + v3 + v4;
		}
		
		for(int i = 0; i < viabilidade.length; i++) {
			
			if(defineAcao(viabilidade[i],i) == "VENDA") {
				
				int qtde = minhaConta.getQtdeAcoes()[i];
				realizaSolicitacao("VENDA", ativosAtuais.get(i).getId(), qtde, 5);
			}
			
			if (defineAcao(viabilidade[i],i) == "COMPRA") {
				
				int qtde = (int) ((minhaConta.getSaldo()*0.6) / (ativosAtuais.get(i).getClose().get(ind)));
				realizaSolicitacao("COMPRA", ativosAtuais.get(i).getId(), qtde, 5);
			} 
		}
	}
	
	private String defineAcao(int viabilidade, int i) {
		
		String acao = null;
		// SE ATIVO TIVER VIABILIDADE ALTA -> COMPRA
		// SE ATIVO TIVER VIABILIDADE BAIXA -> VENDE
	
		// SE J� HOUVE MOVIMENTA��O NA CONTA
			if(minhaConta.getMovimentacao().size()>0) {
				
				// ULTIMO ATIVO MOVIMENTADO
				if(identificaAtivo(minhaConta.getUltimaOperacao().getAtivo()) == i){
					
					if(minhaConta.getUltimaOperacao().getTipoOperacao() == "COMPRA") {
						if(viabilidade < 4 && minhaConta.getQtdeAcoes()[i]>0) {
							acao = "VENDA";
						}
					} else {
						if(viabilidade >= 4) {
							acao = "COMPRA";
						} else { acao = "N/A"; }
					}
				
				} else {			// DEMAIS ATIVOS
					if(viabilidade <= 4 && minhaConta.getQtdeAcoes()[i]>0) {
						acao = "VENDA";
					} else if(viabilidade > 4) {
						acao = "COMPRA";
					} else { acao = "N/A"; }
				}
			} else {				
			// SE AINDA NAO HOUVE MOVIMENTA��O
				if(viabilidade >= 4) {
					acao = "COMPRA";
				} else { acao = "N/A"; }
			}
			
		return acao;
	}
	
	public int identificaAtivo(char a) {
		
		ArrayList<Ativos> ativosAtuais = minhaCorretora.consultaAtivos();

		for(int i = 0; i < ativosAtuais.size(); i++) {
			if(ativosAtuais.get(i).getId() == a) {
				return i;
			}
		}
		
		return 5;
	}
	
	// METODO QUE REALIZA SOLICITA��O DE COMPRA/VENDA DE ATIVOS
	private void realizaSolicitacao(String tipoOp, char ativo, int qtdeAtivos, int prioridade) throws InterruptedException {

		Thread.currentThread().setPriority(prioridade);

		// somente faz movimenta��o na conta, se autorizado pela corretora
		minhaCorretora.solicitacaoCliente(tipoOp, ativo, qtdeAtivos, this.id);
		Thread.sleep(500);			// aguarda 500ms
		
		Thread.currentThread().setPriority(5);
	}
	
	// METODO QUE RETORNAR CONTA CORRENTE
	public ContaCorrente getCC() {
		return minhaConta;
	}
	
	public static void extratoClientes() {
		
		for(int j = 0; j < Cliente.getListaClientes().size(); j++) {
			
			ArrayList<Operacao> operacoes = Cliente.getListaClientes().get(j).getCC().getMovimentacao();
			int nCompra = 0, nVenda = 0; 
			double totalCompra = 0, totalVenda = 0;

			for(int i = 0; i < operacoes.size(); i++) {

				if(operacoes.get(i).getTipoOperacao() == "COMPRA") {
					totalCompra += operacoes.get(i).getValorTotal();
					nCompra++;

				} else if (operacoes.get(i).getTipoOperacao() == "VENDA"){
					totalVenda += operacoes.get(i).getValorTotal();
					nVenda++;
				}
			}
			
			System.out.println("CLIENTE: "+ (j+1)  + " realizou "+ nCompra+" compras de ativos no valor de "+totalCompra+ " reais. \n"
					+ "e "+nVenda+ " vendas de ativos no valor de "+totalVenda+" reais");
		}
	}
	
	
}
