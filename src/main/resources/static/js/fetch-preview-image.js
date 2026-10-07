function fetchPreviewImage() {
    fetch('/settings/preview_image')
        .then(response => response.blob())
        .then(blob => {
            document.getElementById('preview_image').src = URL.createObjectURL(blob);
        })
    .catch(error => console.error('Error loading image:',error));
}