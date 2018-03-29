# stripe-clj

A Clojure(Script) wrapper of the [Stripe API](https://stripe.com/docs/api).

## Documentation

Run `lien codox` in your project directory.

Then open `your-project-directory/target/doc/index.html` in your browser.


## Difference from `racehub/stripe-clj`

This library is forked from
[racehub/stripe-clj](https://github.com/racehub/stripe-clj), but has been
modified so substantially that it shares very little in common with the
original. Most significantly:

- Uses Clojure `spec` instead of [Schema](https://github.com/plumatic/schema)
- Is written for a much newer version of the Stripe API, specifically version 2018-02-06

## Status

This library is under active development. Not recommended for use.

## Testing

- [ ] TODO: Is this still the case? Yes....

You'll need this in your environment:

```sh
export STRIPE_SECRET="stripe_secret_token"
```

You should probably use your dev token.

## Authors

- Josh Lehman <https://github.com/jalehman>
- Therese Diede <https://github.com/tdiede>
- Andres Pineda <https://github.com/humbamp123>

## Credit

Thanks to:

- Sam Ritchie <https://twitter.com/sritchie>
- Dave Petrovics <https://github.com/dpetrovics>

For writing the [racehub/stripe-clj](https://github.com/racehub/stripe-clj) that
this library is inspired by and forked from.

## License

Copyright © 2018 Starcity Properties, Inc

Distributed under the Eclipse Public License.
