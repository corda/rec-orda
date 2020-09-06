import { createTokensContext } from '@arhamill/cortex';

const recTokens = createTokensContext("http://localhost:8080")
export const recTokensContext = recTokens.tokensContext
export const RecTokensProvider = recTokens.TokensProvider