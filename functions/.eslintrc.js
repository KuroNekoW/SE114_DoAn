module.exports = {
    "env": {
        "es6": true,
        "node": true,
    },
    "parserOptions": {
        "ecmaVersion": 2020,
    },
    "extends": [
        "eslint:recommended",
        "google",
    ],
    "rules": {
        "quotes": ["error", "double"],
        "indent": ["error", 4],
        "max-len": ["error", { "code": 120 }],
        "object-curly-spacing": ["error", "always"],
    },
};
