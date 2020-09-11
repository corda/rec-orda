# Recorda
It is a truth universally acknowledged that a single plant in possession of a renewable MWh must be in want of a REC


## Why should we use FungibleRECTokens instead of FungibleTokens
There are two useful git tags: **fungibleRECToken** and **fungibleToken**. 
Both are built on top of the RECToken TokenType. 

#### fungibleRECToken tag
You can find functioning code that uses a customised FungibleToken.
You can run contract code in FungibleRECTokenContract.
Move takes a list of input and list of output tokens. 
Redeem takes a list of tokens to redeem. 

It is harder to work with as you must provide both the input and output tokens.
However, it gives you a lot more flexibility in what you can actually do!
In the future, a good idea would be to write subFlows that would mimic the FungibleToken flows. 
However, change the logic so that it does what one expects it to do. 
For example, you could make Move take a party to whom to move tokens to, a quantity to move,
a list of EnergySource and a boolean. The list of energy source would be the source of tokens you
want to prioritise moving. The boolean would specify whether you can only move those tokens or whether you may 
move others as well, should you run out of the tokens from the source specified in the list. 
For example, [WIND] and false would throw an exception if you try to move 10 but you only have 5 in your vault.
But [WIND] and true would not throw an exception if you had only 5 tokens from WIND but also 10 tokens from SOLAR,
it would take 5 more from SOLAR and move those. However, it will take 5 from WIND first and only when running out of WIND
would take SOLAR tokens. 

#### fungibleToken tag
This code uses FungibleTokens only. It is very limiting. 
If you want to move 5 tokens from WIND and 5 tokens from SOLAR, then you must execute two flows on corda.
Each will execute a move. In terms of complexity, it is worse than using a FungibleRECToken. 

The flow code is much simpler, in fact Move and Redeem are one line long. 
However, there is a trade-off as it means the engineer in charge of writing the call to the flow,
the client or backend logic, will have to initiate multiple flows to execute the move. 
Furthermore, the example discussed above, where you would provide arguments such as [WIND] and true,
would still have to be written, but as a separate function. All in all, apart from a one liner, there is not much to gain
from using a FungibleToken. Looking back at the Move and Redeem flow code for FungibleRECToken, those two flows were only a few
lines long. 
I will leave this tag available for future engineers interested in playing around with this.
However, I believe it is better and ultimately simpler, to use FungibleRECTokens. 