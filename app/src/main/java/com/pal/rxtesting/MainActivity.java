package com.pal.rxtesting;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.observables.ConnectableObservable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Filter number events
        Observable.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .filter(integer -> integer % 2 == 0)
                .subscribe(int_filtered -> log(String.valueOf(int_filtered)));
        //.subscribe(System.out::println);

        //Iterating with "forEach" : forEach( ) — invoke a function on each item emitted by the Observable; block until the Observable completes
        Observable
                .just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .forEach(System.out::println);

        //Group by : divide an Observable into a set of Observables that emit groups of items from the original Observable, organized by key
        Observable.just(1, 2, 3, 4, 5).groupBy(integer -> integer % 2 == 0)
                .subscribe(grouped -> grouped.toList().subscribe(integers -> log(integers + " Even: "+grouped.getKey())));
        // [1, 3, 5] (Even: false)
        // [2, 4] (Even: true)

        //Take only the first N values emitted
        Observable.just(1, 2, 3, 4)
                .take(2) //first(), last(), distinct()
                .subscribe(System.out::println);
        //.subscribe(integer -> {log(integer+"");});

        //Map() : transform the items emitted by an Observable by applying a function to each of them
        Observable.just("Hello world")
                .map(s -> s.hashCode()).subscribe(integer -> log(String.valueOf(integer)));

        //Iterate an array list
        List<String> names = new ArrayList<>();

        names.add(new String("jon snow"));
        names.add(new String("arya stark"));

        //flatMap( ), concatMap( ), and flatMapIterable( ) — transform the items emitted by an Observable into Observables (or Iterables),
        //then flatten this into a single Observable
        Observable.just(names)
                .concatMap(strings -> Observable.from(strings))
                .subscribe(s -> log(s));

        Observable<String> concatenatedStrings = Observable.just("1/7/88", "3/1/14", "6/4/91");
        concatenatedStrings.flatMap(s -> Observable.from(s.split("/"))).subscribe(s1 -> log(s1));

        //cold observable
        //they will "replay" the emissions to each Subscriber. They will repeat the emissions each time they are subscribed to
        //ideal for pushing data as every Subscriber will get all the data emissions
        Observable<String> items = Observable.just("Alpha", "Beta", "Gamma", "Delta");
        items.subscribe(s -> log(s));
        items.map(s -> s.length()).subscribe(integer -> log(Integer.toString(integer)));
        //output : Alpha Beta Gamma Delta 5 4 5 5

        //hot observables
        //start firing "live" emissions and will not replay missed items to new Subscribers.
        //This is appropriate for events like Button clicks, network requests, and other entities representing "events" rather than "data"

        //convert cold observable into hot observable by making it a ConnectableObservable. Call any Observable's publish() method,
        //set up its Subscribers, and then call connect() to fire the emissions hotly all at once.

        ConnectableObservable<String> items2 = Observable.just("Alpha","Beta","Gamma","Delta","Epsilon").publish();

        items2.subscribe(s -> System.out.println(s));
        items2.map(s -> s.length()).subscribe(i -> System.out.println(i));

        items2.connect();
        //output Alpha 5 Beta 4 Gamma 5 Delta 5 Epsilon 7

        /**
         * Explanation by Nickolay Tsvetinov, author of Learning Reactive Programming with Java 8
         *
         * We can say that cold Observables generate notifications for each subscriber and hot Observables are always running,
         * broadcasting notifications to all of their subscribers. Think of a hot Observable as a radio station. All of the
         * listeners that are listening to it at this moment listen to the same song. A cold Observable is a music CD. Many
         * people can buy it and listen to it independently.
         * */

        //moar exemples combining both
        Observable<String> items3 = Observable.just("Alpha", "Beta", "Gamma", "Delta", "Epsilon");

        ConnectableObservable<Long> timer = Observable.interval(1, TimeUnit.SECONDS).publish();

        Subscriber<List<Integer>> subscriber = new Subscriber<List<Integer>>() {
            @Override
            public void onCompleted() {
                log("done");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(List<Integer> integers) {
                System.out.println(integers);
            }
        };

        timer.flatMap(t -> items3.map(s -> s.length()).toList()).
                subscribe(subscriber /*or use: integers -> System.out.println(integers)*/);


        Subscription subscription = timer.connect(); //connect: instruct a connectable Observable to begin emitting items to its subscribers

        try {
            Thread.sleep(5000);

            subscriber.onCompleted();
            subscriber.unsubscribe();
            // we could also use the object "subscription" to unsubscribe : subscription.unsubscribe();

            //output
            // [5, 4, 5, 5, 7]
            // [5, 4, 5, 5, 7]
            // [5, 4, 5, 5, 7]
            // [5, 4, 5, 5, 7]
            // done

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //subscriber with all events (onNext, onError, onCompleted
        Observable.just("Alpha","Beta","Gamma","Delta","Epsilon") //calls onNext() on map()
                .map(s -> s.length()/* == String::length */) //calls onNext() on filter()
                .filter(i -> i <= 5) //calls onNext() on subscriber
                .subscribe(
                        integer -> log(Integer.toString(integer)), /* == System.out::println */
                        throwable -> {
                            log(throwable.toString());
                            throwable.printStackTrace();/* == Throwable::printStackTrace*/
                        },
                        () -> System.out.println("onCompleted!")
                );



        //RxBinding

        //View click
        Button button = (Button) findViewById(R.id.button);
        RxView.clicks(button).subscribe(aVoid -> log("clicked!"));

        //Observe text changes on an EditText (RxBinding)
        EditText editText = (EditText) findViewById(R.id.edit_text);
        EditText editText2 = (EditText) findViewById(R.id.edit_text_2);
        Button bttnLogin = (Button) findViewById(R.id.login);
        //RxTextView.textChangeEvents(editText).subscribe(textViewTextChangeEvent -> log(textViewTextChangeEvent.text().toString()));

        //Filter text changes on an EditText
        //RxTextView.textChangeEvents(editText).filter(s -> s.text().length() > 3).subscribe(textFiltered -> log(textFiltered.text().toString()));
        // => "sea"
        // => "sear"
        // => "searc"
        // => "search"

        //Login form
        Observable<TextViewTextChangeEvent> emailChangeObservable = RxTextView.textChangeEvents(editText);
        Observable<TextViewTextChangeEvent> passwordChangeObservable = RxTextView.textChangeEvents(editText2);

        bttnLogin.setEnabled(false);

        Observable.combineLatest(emailChangeObservable, passwordChangeObservable, (emailObservable, passwObservable) -> {
            log("email "+emailObservable.text().toString() + ", passw " + passwObservable.text().toString());
            boolean emailCheck = emailObservable.text().toString().length() > 3;
            boolean passwCheck = passwObservable.text().toString().length() > 3;

            boolean check = emailCheck && passwCheck;
            log("check? " + String.valueOf(check));
            return check;
        }).subscribe(check -> bttnLogin.setEnabled(check));

    }

    private void log(String message) {
        Log.i(TAG, message);
    }

}
