import java.util.*;

public class Game
{

    private final int DEALER_STAND_THRESHOLD = 17;
    private Collection<Player> players = new ArrayList<>();
    private Dealer dealer;
    private Deck deck;

    enum Option {HIT, STAND, DOUBLEDOWN, SPLIT}

    Game(Dealer dealer, Player... player){
        players.addAll(Arrays.asList(player));
        this.dealer = dealer;
        deck =  new Deck();
    }


    void test(){
        for(Player p : players){
            System.out.println(p.wealth);
        }
    }

    void play()
    {
        round();
        //TODO:shuffle deck
    }

    private String currentSituation(Collection<Player> players){
        StringBuilder sb = new StringBuilder();
        for(Player p : players){
            sb.append(p.toString()).append(": ").append(p.hand.toString()).append("\n");
        }
        return sb.toString();
    }

    private void checkBusted(Set<Player> players){

        Iterator<Player> it = players.iterator();
        while(it.hasNext()){
            Player p = it.next();
            if(p.hand.isBusted())
                it.remove();
        }
    }

    private boolean checkBlackJack(Collection<Player> players){
        Collection<Player> playersWithBlackJack = new ArrayList<>();
        boolean foundBlackJack = false;
        for(Player p : players){
            if(p.hand.isBlackJack()) {
                playersWithBlackJack.add(p);
                foundBlackJack = true;
            }
        }

        if(foundBlackJack) {
            players.clear();
            players.addAll(playersWithBlackJack);
        }

        return foundBlackJack;
    }

    private boolean hitOrStandLoop(Player player, Hand hand){
        //returns false if the player busts and true if he doesn't
        boolean again = true;

        while(again)
        {
            hand.hit(deck.popCard());
            System.out.println("His hand is now: " + hand.toString());
            if(hand.isBusted())
            {
                System.out.println(player.getName() + " you busted!");
                return false;
            }
            else
            {
                System.out.println(player.getName() + " would you like to hit again?(Y/N)");
                String option = Main.userInput();
                if (option.equals("Y"))
                {
                    System.out.println(player.getName() + " chose to hit again");
                    again = true;
                }
                else
                {
                    System.out.println(player.getName() + " chose to stand");
                    again = false;
                }
            }
        }
        return true;
    }

    private boolean split(Player p)
    {

        return false;
    }

    private boolean doubleDown(Player p){
        p.bet(p.getCurrentBet());
        p.hand.doubleDown(deck.popCard());

        return p.hand.isBusted();

    }


    private void round()
    {
        //initially all players are winners
        List<Player> winners = new ArrayList<>(this.players);

        //players bet
        for(Player p : players){
            System.out.println(p.getName() + ": place your bet.");
            int bet = Integer.parseInt(Main.userInput());
            p.bet(bet);
            dealer.giveMoney(bet);
        }

        //initialy give two cards to each player
        for(Player p : players){
            p.giveHand(deck.popCard(), deck.popCard());
        }
        //and one to the dealer
        dealer.giveHand(deck.popCard());

        System.out.println("Dealer: " + dealer.hand.toString());
        System.out.println(currentSituation(players));


        //check if there are any players with blackjack
        boolean foundBlackJack = checkBlackJack(winners);

        if(!foundBlackJack)    //check for options only if there are no blackjacks
        {
            ListIterator<Player> it = winners.listIterator();

            while(it.hasNext())
            {
                Player p = it.next();
                System.out.println(p.toString() + ", what is your option? (HIT, SPLIT, STAND, DOUBLEDOWN");
                Option op = Option.valueOf(Main.userInput());
                System.out.print(p.getName() + " chose to ");
                switch (op) {
                    case HIT:
                        //if the player busts after the hit he is removed from the winners list
                        if (!hitOrStandLoop(p, p.hand)) {
                            it.remove();
                            dealer.takeMoney(p);
                        }
                        break;
                    case SPLIT:
                        if (p.hand.canSplit()) {
                            //take a card away from the current hand
                            p.hand.split();
                            //make a new player and add him to the winners list
                            Player clone = new SplitPlayer(p);

                            //hit for each of them
                            p.hand.hit(deck.popCard());
                            clone.hand.hit(deck.popCard());

                            System.out.println(p.getName() + "split. His new hands are: ");
                            System.out.println(p.hand.toString());
                            System.out.println(clone.hand.toString());

                            //the clone gets added before the current element
                            it.add(clone);
                            it.previous(); //clone
                            it.previous(); //current element


                        } else
                            System.out.println("Can't split, cards aren't equal");
                        break;
                    case STAND:
                        p.hand.stand();
                        break;
                    case DOUBLEDOWN:
                        if(!doubleDown(p)){
                            it.remove();
                            dealer.takeMoney(p);
                        }
                        break;
                    default:
                        System.out.println("Please select a valid option.");
                        break;
                }
            }
        }


        //give another hand to the dealer
        dealer.giveCard(deck.popCard());
        System.out.println("Dealer's hand is: " + dealer.hand.toString());

        //check for blackjack for dealer
        if(dealer.hand.isBlackJack())
        {
            System.out.println("Dealer blackjacked!");
            if(foundBlackJack){
                System.out.print("Tie with: ");
                for(Player p : winners)
                    System.out.print(p.getName() + " ");
            }
            else{
                System.out.println("There are no other blackjacks. Dealer wins.");
            }

            return; //round finished
        }

        //complete the drawing for the dealer
        while(dealer.hand.getValue() < DEALER_STAND_THRESHOLD)
        {
            System.out.println("Dealer hits.");
            dealer.hand.hit(deck.popCard());
            System.out.println("Dealer's hand is: " + dealer.hand.toString());
            if(dealer.hand.isBusted())
            {
                System.out.print("Dealer busted! ");
                for(Player p : winners){
                    System.out.print(p.getName() + " ");
                    dealer.payBet(p);
                }
                System.out.println("win.");
                return; //round finished
            }
        }

        //finally, see who the winner is
        int dealerValue = dealer.hand.getValue();
        for(Player p : winners)
        {
            if(p.hand.getValue() > dealerValue){
                dealer.payBet(p);
                System.out.println(p.getName() + " wins. He receives " + p.getCurrentBet() + ". ");
            }
            else if(p.hand.getValue() == dealerValue){
                dealer.giveBackBet(p);
                System.out.println(p.getName() + " is at a tie with the dealer. His bet is given back.");
            }
            else{
                System.out.println(p.getName() + " loses. He loses his bet. ");
            }
        }


    }




}