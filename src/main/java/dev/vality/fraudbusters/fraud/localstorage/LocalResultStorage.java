package dev.vality.fraudbusters.fraud.localstorage;

import dev.vality.fraudbusters.domain.CheckedPayment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LocalResultStorage {

    private final ThreadLocal<List<CheckedPayment>> localStorage = ThreadLocal.withInitial(ArrayList::new);

    public List<CheckedPayment> get() {
        return localStorage.get();
    }

    public void clear() {
        localStorage.get().clear();
    }

    public void remove() {
        localStorage.remove();
    }

}
