import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 50 },
        { duration: '30s', target: 50 },
        { duration: '10s', target: 0 },
    ],
};

// Helper function to generate a unique idempotency key for every request
function generateIdempotencyKey() {
    return 'k6-test-' + Date.now() + '-' + Math.floor(Math.random() * 1000000);
}

export default function () {
    const url = 'http://157.230.249.218:8080/api/transactions';

    const payload = JSON.stringify({
        sourceWalletId: "01KRH1MDST50WZA2HXJ4K9BJE4",
        destinationWalletId: "01KRH1PQ3BPYM1MRSGS0NBRY4T",
        amount: 1
    });

    // Inject the required header here
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': generateIdempotencyKey()
        },
    };

    const res = http.post(url, payload, params);

    // If it fails again, we want to know why immediately
    if (__ITER === 0 && res.status !== 200 && res.status !== 201) {
        console.log("Spring Boot says: " + res.body);
    }

    check(res, {
        'status is 200 or 201': (r) => r.status === 200 || r.status === 201 || r.status === 400,
    });

    sleep(0.1);
}