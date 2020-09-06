import React, { useContext, useState, useEffect } from "react";
import logo from "./logo.svg";
import "./App.css";
import { recTokensContext } from "./context";

function App() {
  const tok = useContext(recTokensContext);
  const [recToken, setRecToken] = useState([])

  useEffect(() => {
    setRecToken(tok[0])
  }, [tok])

  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          Edit <code>src/App.js</code> and save to reload.
        </p>
        { recToken ? 
        <h1> Number of Tokens: {recToken.quantity}</h1> : null }
        <a
          className="App-link"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn React
        </a>
      </header>
    </div>
  );
}

export default App;
