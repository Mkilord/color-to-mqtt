function fetchPreviewImage() {
    fetch('/settings/preview_image')
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.blob();
        })
        .then(blob => {
            let image = document.getElementById('preview_image');
            image.src = URL.createObjectURL(blob);
            image.style.display = 'block';
        })
        .catch(error => console.error('Error loading image:', error));
}
