let isStarted = false;

document.addEventListener('DOMContentLoaded', () => {
    let status = document.getElementById('app-status').getAttribute('data-status');
    console.log("статус:" + status);
    if (status === 'true') {
        setStartState();
        updateColorLoop();
        console.log("Программа запущена")
    } else {
        setStopState();
        console.log("Программа остановлена!")
    }
});

function start() {
    fetch('/start', {method: 'POST'})
        .then(response => response.text().then(message => ({ok: response.ok, message})))
        .then(({ok, message}) => {
            if (!ok) {
                setStopState();
                showNotification(message, 'error');
                return;
            }
            setStartState()
            showNotification(message, 'success');
            updateColorLoop()
        }).catch(error => {
        isStarted = false;
        showNotification('Ошибка при старте: ' + error, 'error');
    })
}

function stop() {
    fetch('/stop', {method: 'POST'})
        .then(response => response.text())
        .then((message) => {
            showNotification(message, 'success');
            setStopState()
        })
        .catch(error => {
            console.log(error)
            setStopState()
        })
}

function setStopState() {
    document.getElementById('startBtn').classList.remove('start-active');
    isStarted = false;
}

function setStartState() {
    isStarted = true;
    document.getElementById('startBtn').classList.add('start-active');
}

function isStart() {
    return isStarted;
}

function updateColorLoop() {
    if (!isStart()) return;

    fetch('/color')
        .then(response => response.text())
        .then(color => {
            console.log(color);
            document.getElementById('color-box').style.backgroundColor = color;
            document.getElementById('color-description').textContent = color;
        }).catch(() => {
        setStopState()})
        .finally(() => {
            if (isStart()) {
                setTimeout(updateColorLoop, 500);
            }
        });
}

function showNotification(message, type) {
    let notification = document.getElementById('notification');

    notification.textContent = message;

    if (type === 'success') {
        notification.style.backgroundColor = '#4CAF50';
    } else if (type === 'error') {
        notification.style.backgroundColor = '#f44336';
    }

    notification.classList.add('show');

    setTimeout(function () {
        notification.classList.remove('show');
    }, 3000);
}
