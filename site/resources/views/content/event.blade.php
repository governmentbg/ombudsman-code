@extends('layouts.default')
@section('title', $data->FqL_title . ' - ' . trans('common.title'))
@section('site-description', $data->FqL_title)
@section('site-keywords', trans('page-keywords'))
{{-- @section('site-canonical', $canonical) --}}
{{-- @section('page-hreflang', $canonical) --}}
@section('article-headline', $data->FqL_title)
{{-- {{-- @section('article-date', $publishDate) --}}
@section('article-modification-date', $data->created_at)
@section('article-description', trans('page-description'))
@section('article-tag', '')


@section('content')
    @include('layouts.breadcrumb')

    <div class="row">
        <div class="col ">
            <h1 class="t-h1">{{ trans('common.pastEvents') }}</h1>
        </div>
    </div>

    {{-- {{ddd($nav["data"])}} --}}
    <div class=" mb-5 m-article">

        <div class="row m-info">
            <div class="col-sm-12">

                <h2 class="">{{ $data->MvL_title }}</h2>
                <div class="">
                    {!! $data->MvL_body !!}
                </div>



                @if ($data->Str_url)
                    <video id="my-video" class="video-js mb-3" controls preload="auto" width="640" height="264"
                        data-setup="{}">
                        <source src="{{ $data->Str_url }}" type="video/mp4" />
                        {{-- <source src="MY_VIDEO.webm" type="video/webm" /> --}}
                        <p class="vjs-no-js">
                            To view this video please enable JavaScript, and consider upgrading to a
                            web browser that
                            <a href="https://videojs.com/html5-video-support/" target="_blank">supports HTML5 video</a>
                        </p>
                    </video>
                @endif
                @if ($data->Str_embed)
                    @if (substr($data->Str_embed, 0, 4) == 'http')
                        <div class="row">
                            <div class="col-sm-12 col-xs-12 col-md-10 col-lg-8 col-xl-6">
                                <x-embed url="{{ $data->Str_embed }}" class="mb-3" />
                            </div>
                        </div>
                    @else
                        {!! $data->Str_embed !!}
                    @endif
                @endif


            </div>






        </div>
    </div>





@stop
