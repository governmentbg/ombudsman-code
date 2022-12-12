<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddLngToMGalleryTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('m_gallery_lng', function (Blueprint $table) {
            $table->integer('S_Lng_id')->comment('Lng ID')->unsigned()->after('ArG_id')->nullable();

            $table->foreign('S_Lng_id')->references('S_Lng_id')->on('s_lang');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('m_gallery_lng', function (Blueprint $table) {
            //
        });
    }
}
