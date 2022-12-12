<div class="m-position">
    <ul class="list">
        @php
        $parentId = \App\Http\Controllers\CommonController::getIdBySegment(Request::segment(3));
        @endphp

        @foreach (\App\Http\Controllers\NavigationController::navObject(App::getLocale(), $parentId, 1, 0) as $nav)
        <li>
            <div class="content">
                <a href="/{{ App::getLocale() }}/p/{{ $nav->ArL_path }}">{{ $nav->ArL_title }}</a>
            </div>

            <div class="m-d"></div>

        </li>
        @endforeach
    </ul>