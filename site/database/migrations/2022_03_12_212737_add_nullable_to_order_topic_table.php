<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddNullableToOrderTopicTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('q_topic', function (Blueprint $table) {
            $table->integer('Qt_order')->default(null)->nullable()->change();
            $table->boolean('Qt_multi')->nullable()->change();
            $table->boolean('Qt_freetext')->nullable()->change();
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('q_topic', function (Blueprint $table) {
            //
        });
    }
}
